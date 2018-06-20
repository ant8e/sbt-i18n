package tech.ant8e.sbt.i18n

import com.typesafe.config.{Config, ConfigUtil}
import tech.ant8e.sbt.i18n.BundleEmitter._

import scala.annotation.tailrec
import scala.collection.JavaConverters._
case class BundleEmitter(config: Config, packageName: String) {

  val languages =
    config
      .root()
      .keySet()
      .asScala
      .toSet

  val translationKeys =
    languages.foldRight(Set.empty[String]) {
      case (lang, acc) => acc ++ translationKeysOf(config.getConfig(lang))
    }

  val tree = buildTree()

  def emit(): String =
    s"""package $packageName
       |
       |object Bundle {
       | ${emitStructure()}
       | ${languages.map(emitValues).mkString("\n")}
       |}
     """.stripMargin

  private[i18n] def translationKeysOf(c: Config): Set[String] =
    c.entrySet()
      .asScala
      .map(_.getKey)
      .toSet

  private[i18n] def emitStructure(): String = {

    def emitKeyDef(key: String) = s"def ${ScalaIdentifier.asIdentifier(key)}: String"
    //@tailrec
    def emit_(t: Branch): String =
      t.children
        .map {
          case Message(key, _) => emitKeyDef(key)
          case b @ Branch(key, _) =>
            s"""abstract class ${ScalaIdentifier.asIdentifier(key.capitalize)} {
             |${emit_(b)}
             |}
             |
             |def ${ScalaIdentifier.asIdentifier(key)}: ${ScalaIdentifier.asIdentifier(
                 key.capitalize)}
             |""".stripMargin
        }
        .mkString("\n")

    s"""abstract class I18N {
       |${emit_(tree)}
       |}""".stripMargin

  }

  private[i18n] def emitValues(lang: String): String = {

    def emitKeyVal(key: String, value: String) =
      s"val ${ScalaIdentifier.asIdentifier(key)}= ${quote(value)}"
    //@tailrec
    def emit_(t: Branch, path: String): String =
      t.children
        .map {
          case Message(key, messages) =>
            emitKeyVal(key, messages.getOrElse(lang, s"??? $path.$key ???"))
          case b @ Branch(key, _) =>
            s"""object ${ScalaIdentifier.asIdentifier(key)} extends  ${ScalaIdentifier.asIdentifier(
                 key.capitalize)} {
             |${emit_(b, s"$path.$key")}
             |}
             |""".stripMargin
        }
        .mkString("\n")

    s"""object ${ScalaIdentifier.asIdentifier(lang)} extends I18N {
       |${emit_(tree, lang)}
       |}""".stripMargin

  }

  private[i18n] def buildTree() =
    languages.foldRight(new Root()) {
      case (lang, tree) =>
        config.getConfig(lang).entrySet().asScala.foldRight(tree) {
          case (entry, langTree) =>
            updateTree(langTree,
                       lang,
                       entry.getValue.unwrapped().toString,
                       ConfigUtil.splitPath(entry.getKey).asScala.toList)
        }
    }

  private[i18n] def updateTree(tree: Root,
                               lang: String,
                               rawValue: String,
                               path: List[String]): Root = {
    def updateTree_(t: Branch, subPath: List[String]): Branch = (t, subPath) match {

      case (b @ Branch(_, children), head :: Nil) =>
        val subChild = children.find(_.key == head).getOrElse(Message(head, Map.empty))
        val newSub = subChild match {
          case m: Message => m.copy(messages = m.messages + (lang -> rawValue))
          case _          => subChild // TODO mergeError
        }
        b.copy(children = b.children - subChild + newSub)

      case (cb @ Branch(_, children), head :: tail) =>
        val subChild = children.find(_.key == head).getOrElse(Branch(head, Set.empty))
        val t = subChild match {
          case b: Branch => updateTree_(b, tail)
          case _         => subChild // TODO mergeError
        }
        cb.copy(children = cb.children + t)

      case _ => t
    }

    new Root(updateTree_(tree, path).children)

  }
}

object BundleEmitter {
  private[i18n] def pathKeys(c: Set[String]): Set[String] =
    c.map(path => ConfigUtil.splitPath(path).asScala)
      .collect { case p if p.length > 1 => ConfigUtil.joinPath(p.init.asJava) }

  private[i18n] def splitKeys(c: Set[String]): (Set[String], Set[String]) =
    c.partition(k => ConfigUtil.splitPath(k).asScala.length > 1)

  private[i18n] val ___root__ = "___root___"
  sealed abstract class Node(val key: String)
  class Root(override val children: Set[Node] = Set.empty)                    extends Branch(___root__, children)
  case class Branch(override val key: String, children: Set[Node])            extends Node(key)
  case class Message(override val key: String, messages: Map[String, String]) extends Node(key)

  private[i18n] def quote(s: String) = {
    val q = """""""""
    s"$q$s$q"
  }
}
