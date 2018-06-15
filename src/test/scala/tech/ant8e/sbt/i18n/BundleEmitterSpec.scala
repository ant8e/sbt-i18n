package tech.ant8e.sbt.i18n

import java.text.MessageFormat

import com.typesafe.config.ConfigFactory
import org.scalatest.{FlatSpec, Matchers}

class BundleEmitterSpec extends FlatSpec with Matchers {
  private val packageName = "org.example.i18n"

  "A Bundle Emitter" should "emit the correct package" in {
    val expected =
      """package """ + packageName + """
        |
        |object Bundle {}
      """.stripMargin

    BundleEmitter(ConfigFactory.empty(), packageName).emit() should be(expected)
  }

  it must "identify the languages keys" in {
    val configString =
      """
      |fr {
      |    text = Bonjour
      |}
      |de {
      |    text = Guttentag
      |}
      |""".stripMargin

    val config = ConfigFactory.parseString(configString)

    BundleEmitter(config, packageName).findLanguages() should be(Set("fr", "de"))
  }

  it must "identify all the translations keys" in {
    val configString =
      """
      |fr {
      |    text = Bonjour
      |
      |    topic {
      |      key1 = Salade
      |    }
      |}
      |
      |de {
      |    text  = Guttentag
      |    text2 = ich heiße MARVIN
      |}
      |""".stripMargin
    val config = ConfigFactory.parseString(configString.stripMargin)
    BundleEmitter(config, packageName)
      .findTranslationKeys() should be(Set("text", "topic.key1", "text2"))
  }

  it must "identify all the path keys" in {
    BundleEmitter.pathKeys(Set("text", "topic.key1", "text2")) should be(Set("topic"))
  }
}

object Bundle {

  abstract class I18N {
    abstract class Topic {
      abstract class SubTopic {
        def aaa: String
      }
      def zaza: String
    }

    def topic: Topic
    def toto: String
    def titi(arg1: String): String
  }

  object fr extends I18N {
    override def toto: String               = "tutu"
    override def titi(arg1: String): String = MessageFormat.format("%s", arg1)

    object topic extends Topic {
      val zaza: String = "zaza"
    }
  }

  object de extends I18N {
    override def toto: String               = "tutu"
    override def titi(arg1: String): String = MessageFormat.format("%s", arg1)

    object topic extends Topic {
      val zaza: String = "zaza"
    }
  }

  private val mappings               = Map("fr" -> fr)
  def apply(s: String): Option[I18N] = mappings.get(s)

}
