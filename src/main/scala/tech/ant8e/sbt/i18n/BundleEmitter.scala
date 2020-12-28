package tech.ant8e.sbt.i18n

import java.text.{DateFormat, Format, MessageFormat, NumberFormat}

import com.typesafe.config.{Config, ConfigUtil}
import tech.ant8e.sbt.i18n.BundleEmitter.Param._
import tech.ant8e.sbt.i18n.BundleEmitter._

import scala.collection.JavaConverters._
import scala.util.Try
case class BundleEmitter(config: Config, packageName: String, breakOnMissingKeys: Boolean = false) {

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
       |object Bundle {
       | ${emitMap()}
       | ${emitStructure()}
       | ${languages.map(emitValues).mkString("\n")}
       |}
     """.stripMargin

  private[i18n] def translationKeysOf(c: Config): Set[String] =
    c.entrySet()
      .asScala
      .map(_.getKey)
      .toSet

  private def toScalaType(paramType: ParamType) =
    paramType match {
      case StringParam => "String"
      case DateParam   => "java.util.Date"
      case DoubleParam => "Double"
      case LongParam   => "Long "
    }

  private[i18n] def emitStructure(): String = {

    def emitKeySimpleDef(key: String) = s"def ${ScalaIdentifier.asIdentifier(key)}: String"
    def emitKeyParamDef(key: String, paramTypes: List[ParamType]) = {

      val params = paramTypes.zipWithIndex
        .map { case (ptype, index) => s"x$index: ${toScalaType(ptype)}" }
        .mkString(",")

      s"def ${ScalaIdentifier.asIdentifier(key)}($params): String"
    }

    def emit_(t: Branch): String =
      t.children.toList
        .sortBy(_.key) // ensure the generated string are in the order of the orig set
        .map {
          case SimpleMessage(key, _)                   => emitKeySimpleDef(key)
          case ParametrizedMessage(key, _, paramsType) => emitKeyParamDef(key, paramsType)
          case b @ Branch(key, _)                      =>
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
       |${emit_(tree)}
       |}""".stripMargin

  }

  private[i18n] def emitMap(): String =
    "val languages: Map[String, I18N] = Map(" + languages
      .map(language => s"""("$language", $language)""")
      .mkString(", ") + ")"

  private[i18n] def emitValues(lang: String): String = {

    def emitSimpleKeyVal(key: String, value: String) =
      s"val ${ScalaIdentifier.asIdentifier(key)}= ${quote(value)}"

    def emitKeyParamDef(key: String, value: String, paramTypes: List[ParamType]) = {

      val params            = paramTypes.zipWithIndex
        .map { case (ptype, index) => s"x$index: ${toScalaType(ptype)}" }
        .mkString(",")
      val paramsApplication = paramTypes.zipWithIndex
        .map { case (_, index) => s"x$index" }
        .mkString(",")

      s"def ${ScalaIdentifier.asIdentifier(key)}($params): String= java.text.MessageFormat.format(${quote(value)}, $paramsApplication)"
    }

    def emit_(t: Branch, path: String): String =
      t.children.toList
        .sortBy(_.key) // ensure the generated string are in the order of the orig set
        .map {
          case SimpleMessage(key, messages)                   =>
            emitSimpleKeyVal(
              key,
              messages.getOrElse(lang, defaultIfAllowed(s"$path.$key", s"??? $path.$key ???"))
            )
          case ParametrizedMessage(key, messages, paramsType) =>
            emitKeyParamDef(
              key,
              messages.getOrElse(lang, defaultIfAllowed(s"$path.$key", s"??? $path.$key ???")),
              paramsType
            )
          case b @ Branch(key, _)                             =>
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
          val params = BundleEmitter.Param.identifyParams(rawValue)

          if (params.isLeft) {
            // TODO reportError
          }

          val paramTypes = params.getOrElse(List.empty)

          val subChild = children
            .find(_.key == head)
            .getOrElse(
              if (paramTypes.isEmpty)
                SimpleMessage(head, Map.empty)
              else
                ParametrizedMessage(head, Map.empty, paramTypes)
            )

          val newSub = subChild match {
            case m: SimpleMessage       => m.copy(messages = m.messages + (lang -> rawValue))
            case m: ParametrizedMessage => m.copy(messages = m.messages + (lang -> rawValue))
            case _                      => subChild // TODO mergeError
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
  def pathKeys(c: Set[String]): Set[String]                 =
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
      paramsType: List[Param.ParamType]
  )                                                                    extends Node

  def quote(s: String) = {
    val q = """""""""
    s"$q$s$q"
  }

  object Param {

    sealed trait ParamType
    case object StringParam extends ParamType
    case object DateParam   extends ParamType
    case object DoubleParam extends ParamType
    case object LongParam   extends ParamType

    def identifyParams(s: String): Either[Throwable, List[ParamType]] =
      Try {
        new MessageFormat(s).getFormatsByArgumentIndex.toList
          .map(javaTextFormatToParamType)

      }.toEither

    private def javaTextFormatToParamType(format: Format) =
      format match {
        case null                                    => StringParam
        case _: DateFormat                           => DateParam
        case x: NumberFormat if x.isParseIntegerOnly => LongParam
        case _: NumberFormat                         => DoubleParam
        case _                                       => StringParam
      }
  }

}
