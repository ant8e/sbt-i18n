package tech.ant8e.sbt.i18n

import com.typesafe.config.{Config, ConfigUtil}

import scala.collection.JavaConverters._
case class BundleEmitter(config: Config, packageName: String) {

  def emit(): String = s"package $packageName"

  private[i18n] def findLanguages(): Set[String] =
    config.root().keySet().asScala.toSet

  private[i18n] def findTranslationKeys(): Set[String] =
    findLanguages().foldRight(Set.empty[String]) {
      case (key, acc) => acc ++ findTranslationKeys(config.getConfig(key))
    }

  private[i18n] def findTranslationKeys(c: Config): Set[String] =
    c.entrySet().asScala.map(_.getKey).toSet
}
object BundleEmitter {
  private[i18n] def pathKeys(c: Set[String]): Set[String] =
    c.map(path => ConfigUtil.splitPath(path).asScala.toList)
      .collect { case p if p.length > 1 => ConfigUtil.joinPath(p.init.asJava) }

}
