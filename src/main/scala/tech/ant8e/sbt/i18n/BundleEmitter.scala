package tech.ant8e.sbt.i18n

import com.google.common.base.CaseFormat
import com.ibm.icu.text.DateFormat
import com.ibm.icu.text.MessageFormat
import com.ibm.icu.text.MessagePatternUtil
import com.ibm.icu.text.MessagePatternUtil.ArgNode
import com.ibm.icu.text.MessagePatternUtil.MessageNode
import com.ibm.icu.text.MessagePatternUtil.{Node => PatternNode}
import com.ibm.icu.text.MessagePattern
import com.ibm.icu.text.NumberFormat
import com.typesafe.config.Config
import com.typesafe.config.ConfigUtil
import tech.ant8e.sbt.i18n.BundleEmitter._
import tech.ant8e.sbt.i18n.BundleEmitter.Param._

import java.text.Format
import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.collection.immutable.ListMap
import scala.util.Try
case class BundleEmitter(
    config: Config,
    packageName: String,
    breakOnMissingKeys: Boolean = false,
    optionalReturnValues: Boolean = false
) {

  val languages =
    config
      .root()
      .keySet()
      .asScala
      .toSet

  val translationKeys =
    languages.foldRight(Set.empty[String]) { case (lang, acc) =>
      acc ++ translationKeysOf(config.getConfig(lang))
    }

  val tree = buildTree()

  def emit(): String =
    s"""package $packageName
       |
       |import com.ibm.icu.text.MessageFormat
       |import com.ibm.icu.util.Calendar.MONDAY
       |import com.ibm.icu.util.GregorianCalendar
       |import com.ibm.icu.util.TimeZone
       |
       |import java.time.ZonedDateTime
       |import java.time.temporal.ChronoField
       |import java.util.Date
       |import java.util.Locale
       |
       |object Bundle {
       | ${emitMap()}
       | ${emitStructure()}
       | ${languages.map(emitValues).mkString("\n")}
       |
       |  /**
       |    * Backport [[java.util.GregorianCalendar.from]] to ICU GregorianCalendar.
       |    */
       |  private def toCalendar(zdt: ZonedDateTime) = {
       |    val cal = new GregorianCalendar(TimeZone.getTimeZone(zdt.getZone.getId))
       |    cal.setGregorianChange(new Date(Long.MinValue))
       |    cal.setFirstDayOfWeek(MONDAY)
       |    cal.setMinimalDaysInFirstWeek(4)
       |    try {
       |      cal.setTimeInMillis(
       |        Math.addExact(Math.multiplyExact(zdt.toEpochSecond, 1000), zdt.get(ChronoField.MILLI_OF_SECOND))
       |      );
       |    } catch {
       |      case ex: ArithmeticException => throw new IllegalArgumentException(ex)
       |    }
       |    cal
       |  }
       |}
     """.stripMargin

  private[i18n] def translationKeysOf(c: Config): Set[String] =
    c.entrySet()
      .asScala
      .map(_.getKey)
      .toSet

  private def toScalaType(paramType: ParamType) =
    paramType match {
      case AnyParam     => "Any"
      case DateParam    => "ZonedDateTime"
      case DoubleParam  => "Double"
      case LongParam    => "Long"
      case e: EnumParam => e.typeName
    }

  private[i18n] def emitStructure(): String = {
    def emitEnum(e: EnumParam) =
      e.variants
        .map(v => s"case object $v extends ${e.typeName}")
        .mkString(
          s"""sealed trait ${e.typeName}
           |object ${e.typeName} {
           |""".stripMargin,
          "\n",
          "\n}\n"
        )

    def emitReturnType                                    = if (optionalReturnValues) "Option[String]" else "String"
    def emitKeySimpleDef(key: String)                     =
      s"def ${ScalaIdentifier.asIdentifier(key)}: ${emitReturnType}"
    def emitKeyParamDef(key: String, params: List[Param]) = {
      val enums = params.collect { case Param(_, enum: EnumParam) =>
        enum
      }

      val enumsStr = enums.map(emitEnum).mkString("\n")

      val paramsStr = params
        .map(p => s"${p.fieldName}: ${toScalaType(p.paramType)}")
        .mkString(", ")

      s"${enumsStr}def ${ScalaIdentifier.asIdentifier(key)}($paramsStr): ${emitReturnType}"
    }

    def emit_(t: Branch): String =
      t.children.toList
        .sortBy(_.key) // ensure the generated string are in the order of the orig set
        .map {
          case SimpleMessage(key, _)               => emitKeySimpleDef(key)
          case ParametrizedMessage(key, _, params) => emitKeyParamDef(key, params)
          case b @ Branch(key, _)                  =>
            s"""abstract class ${ScalaIdentifier.asIdentifier(key.capitalize)} {
             |${emit_(b)}
             |}
             |
             |def ${ScalaIdentifier.asIdentifier(key)}: ${ScalaIdentifier.asIdentifier(
                key.capitalize
              )}
             |""".stripMargin
        }
        .mkString("\n")

    s"""abstract class I18N {
       |protected val locale: Locale
       |${emit_(tree)}
       |}""".stripMargin

  }

  private[i18n] def emitMap(): String =
    "val languages: Map[String, I18N] = Map(" + languages
      .map(language => s"""("$language", ${ScalaIdentifier.asIdentifier(language)})""")
      .mkString(", ") + ")"

  private[i18n] def emitValues(lang: String): String = {

    def emitSimpleKeyVal(key: String, value: String) =
      s"val ${ScalaIdentifier.asIdentifier(key)}= ${quote(value)}"

    def emitSimpleKeyValOptional(key: String, value: Option[String]) =
      value.fold(s"val ${ScalaIdentifier.asIdentifier(key)}= None")(v =>
        s"val ${ScalaIdentifier.asIdentifier(key)}= Some(${quote(v)})"
      )

    def emitParamsStr(params: List[Param]) =
      params
        .map(p => s"${p.fieldName}: ${toScalaType(p.paramType)}")
        .mkString(", ")

    def emitParamsApplication(params: List[Param]) =
      params
        .map { p =>
          if (p.paramType == DateParam) {
            s""""${p.name}", toCalendar(${p.fieldName})"""
          } else {
            s""""${p.name}", ${p.fieldName}"""
          }
        }
        .mkString("java.util.Map.of(", ", ", ")")

    def emitKeyParamDef(key: String, value: String, params: List[Param]) = {
      val paramsStr         = emitParamsStr(params)
      val paramsApplication = emitParamsApplication(params)

      s"def ${ScalaIdentifier.asIdentifier(key)}($paramsStr): String = new MessageFormat(${quote(value)}, locale).format($paramsApplication)"
    }

    def emitKeyParamDefOptional(key: String, value: Option[String], params: List[Param]) = {
      val paramsStr         = emitParamsStr(params)
      val paramsApplication = emitParamsApplication(params)

      value.fold(s"def ${ScalaIdentifier.asIdentifier(key)}($paramsStr): Option[String] = None")(
        v =>
          s"def ${ScalaIdentifier
              .asIdentifier(key)}($paramsStr): Option[String] = Some(new MessageFormat(${quote(v)}, locale).format($paramsApplication))"
      )
    }

    def emit_(t: Branch, path: String): String =
      t.children.toList
        .sortBy(_.key) // ensure the generated string are in the order of the orig set
        .map {
          case SimpleMessage(key, messages) if optionalReturnValues               =>
            emitSimpleKeyValOptional(key, messages.get(lang))
          case SimpleMessage(key, messages)                                       =>
            emitSimpleKeyVal(
              key,
              messages.getOrElse(lang, defaultIfAllowed(s"$path.$key", s"??? $path.$key ???"))
            )
          case ParametrizedMessage(key, messages, params) if optionalReturnValues =>
            emitKeyParamDefOptional(key, messages.get(lang), params)
          case ParametrizedMessage(key, messages, params)                         =>
            emitKeyParamDef(
              key,
              messages.getOrElse(lang, defaultIfAllowed(s"$path.$key", s"??? $path.$key ???")),
              params
            )
          case b @ Branch(key, _)                                                 =>
            s"""object ${ScalaIdentifier.asIdentifier(key)} extends  ${ScalaIdentifier
                .asIdentifier(key.capitalize)} {
               |${emit_(b, s"$path.$key")}
               |}
               |""".stripMargin
        }
        .mkString("\n")

    def defaultIfAllowed(fullKey: String, default: => String): String =
      if (!breakOnMissingKeys) {
        default
      } else {
        throw new Exception(s"There's missing value for '$fullKey'")
      }

    s"""object ${ScalaIdentifier.asIdentifier(lang)} extends I18N {
       |override protected val locale: Locale = Locale.forLanguageTag("${ScalaIdentifier
        .asIdentifier(lang)}")
       |
       |${emit_(tree, lang)}
       |}""".stripMargin

  }

  private[i18n] def buildTree() =
    languages.foldRight(new Root()) { case (lang, t) =>
      config.getConfig(lang).entrySet().asScala.foldRight(t) { case (entry, langTree) =>
        updateTree(
          langTree,
          lang,
          entry.getValue.unwrapped().toString,
          ConfigUtil.splitPath(entry.getKey).asScala.toList
        )
      }
    }

  private[i18n] def updateTree(
      tree: Root,
      lang: String,
      rawValue: String,
      path: List[String]
  ): Root = {
    def updateTree_(t: Branch, subPath: List[String]): Branch =
      (t, subPath) match {

        case (b @ Branch(_, children), head :: Nil) =>
          val paramsE = BundleEmitter.ParamsExtractor.extractParams(rawValue)

          if (paramsE.isLeft) {
            // TODO reportError
          }

          val params = paramsE.getOrElse(List.empty)

          val subChild = children
            .find(_.key == head)
            .getOrElse(
              if (params.isEmpty)
                SimpleMessage(head, Map.empty)
              else
                ParametrizedMessage(head, Map.empty, params)
            )

          val newSub = subChild match {
            case SimpleMessage(key, messages) if params.nonEmpty =>
              ParametrizedMessage(key, messages + (lang -> rawValue), params)
            case m: SimpleMessage                                =>
              m.copy(messages = m.messages + (lang -> rawValue))
            case m: ParametrizedMessage                          =>
              m.copy(messages = m.messages + (lang -> rawValue))
            case _                                               =>
              subChild // TODO mergeError
          }
          b.copy(children = b.children - subChild + newSub)

        case (cb @ Branch(_, children), head :: tail) =>
          val subChild = children.find(_.key == head).getOrElse(Branch(head, Set.empty[Node]))
          val t        = subChild match {
            case b: Branch => updateTree_(b, tail)
            case _         => subChild // TODO mergeError
          }
          cb.copy(children = cb.children - subChild + t)

        case _ => t
      }

    new Root(updateTree_(tree, path).children)

  }
}

private[i18n] object BundleEmitter {
  def pathKeys(c: Set[String]): Set[String] =
    c.map(path => ConfigUtil.splitPath(path).asScala)
      .collect { case p if p.length > 1 => ConfigUtil.joinPath(p.init.asJava) }

  def splitKeys(c: Set[String]): (Set[String], Set[String]) =
    c.partition(k => ConfigUtil.splitPath(k).asScala.length > 1)

  val ___root__ = "___root___"

  sealed abstract class Node {
    def key: String
  }

  class Root(override val children: Set[Node] = Set.empty[Node])       extends Branch(___root__, children)
  case class Branch(key: String, children: Set[Node])                  extends Node
  case class SimpleMessage(key: String, messages: Map[String, String]) extends Node
  case class ParametrizedMessage(
      key: String,
      messages: Map[String, String],
      params: List[Param]
  ) extends Node

  def quote(s: String) = {
    val q = """""""""
    s"$q$s$q"
  }

  case class Param(name: String, paramType: ParamType) {
    lazy val fieldName: String =
      if (StringUtils.isNumeric(name)) {
        s"x$name"
      } else {
        StringUtils.fieldName(name)
      }
  }

  object Param {

    sealed trait ParamType
    case object AnyParam                                       extends ParamType
    case object DateParam                                      extends ParamType
    case object DoubleParam                                    extends ParamType
    case object LongParam                                      extends ParamType
    case class EnumParam(name: String, variants: List[String]) extends ParamType {
      val typeName: String = if (StringUtils.isNumeric(name)) {
        StringUtils.typeName(variants.mkString("_"))
      } else {
        StringUtils.typeName(name)
      }
    }
  }

  object StringUtils {
    def typeName(s: String): String =
      normalize(s, CaseFormat.UPPER_CAMEL)

    def fieldName(s: String): String =
      normalize(s, CaseFormat.LOWER_CAMEL)

    def isNumeric(s: String): Boolean = s.forall(_.isDigit)

    private def normalize(name: String, format: CaseFormat) =
      if (name.contains("_"))
        CaseFormat.LOWER_UNDERSCORE.to(format, name.replaceAll("\\W", ""))
      else if (name.charAt(0).isLower) CaseFormat.LOWER_CAMEL.to(format, name.replaceAll("\\W", ""))
      else CaseFormat.UPPER_CAMEL.to(format, name.replaceAll("\\W", ""))
  }

  object ParamsExtractor {
    private case class Result(arguments: ListMap[String, Param]) {
      def +:(argument: Param): Result =
        if (!arguments.keySet.contains(argument.name)) {
          val argumentsUpdated = arguments + (argument.name -> argument)
          Result(argumentsUpdated)
        } else {
          val existingArgument = arguments(argument.name)

          (existingArgument.paramType, argument.paramType) match {
            case (t1, t2) if t1 == t2 => this
            // If one of the arguments is Any and another is more specific, use the more specific one
            case (_, AnyParam)        => this
            case (AnyParam, _)        =>
              val argumentsUpdated = arguments - argument.name + (argument.name -> argument)
              Result(argumentsUpdated)
            case _                    => throw new IllegalArgumentException("types mismatch")
          }
        }
    }

    private object Result {
      val empty: Result = new Result(ListMap.empty)
    }

    def extractParams(s: String): Either[Throwable, List[Param]] = {
      @tailrec
      def loop(nodes: List[PatternNode])(acc: Result): Result =
        nodes match {
          case (node: MessageNode) :: tail =>
            val next = node.getContents.asScala.toList

            loop(next ++ tail)(acc)
          case (node: ArgNode) :: tail     =>
            val paramType = getArgumentType(node)
            val argument  = Param(node.getName, paramType)
            val next      = getNext(node)

            loop(next ++ tail)(argument +: acc)
          case _ :: tail                   => loop(tail)(acc)
          case Nil                         => acc
        }

      Try {
        val msgPattern = new MessagePattern(s).freeze()

        val node = MessagePatternUtil.buildMessageNode(msgPattern)

        loop(node :: Nil)(Result.empty).arguments.values.toList
      }.toEither

    }

    private def getArgumentType(node: ArgNode): ParamType =
      node.getArgType match {
        case MessagePattern.ArgType.NONE | MessagePattern.ArgType.SIMPLE =>
          // For simple arguments extract type by checking the param format
          val mf     = new MessageFormat(node.toString)
          val format = mf.getFormats.head
          javaTextFormatToParamType(format)
        case MessagePattern.ArgType.SELECT                               =>
          // Build enum with all values
          val variants = node.getComplexStyle.getVariants.asScala.toList.map(_.getSelector)
          EnumParam(node.getName, variants)
        case _                                                           =>
          // For plural, selectordinal and choice check variant selectors and select between long and double
          val allVariantsInteger =
            node.getComplexStyle.getVariants.asScala.forall(v =>
              !v.isSelectorNumeric || v.getSelectorValue.isWhole
            )
          if (allVariantsInteger) LongParam else DoubleParam

      }

    // extract child messages to put to the loop queue
    private def getNext(node: ArgNode): List[PatternNode] =
      Option(node.getComplexStyle).fold(List.empty[PatternNode]) { cs =>
        cs.getVariants.asScala.toList.map(_.getMessage)
      }

    private def javaTextFormatToParamType(format: Format): ParamType =
      format match {
        case null                                    => AnyParam
        case _: DateFormat                           => DateParam
        case x: NumberFormat if x.isParseIntegerOnly => LongParam
        case _: NumberFormat                         => DoubleParam
        case _                                       => AnyParam
      }
  }

}
