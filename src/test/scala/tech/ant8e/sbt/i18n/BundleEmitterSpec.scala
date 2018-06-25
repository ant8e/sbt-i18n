package tech.ant8e.sbt.i18n

import com.typesafe.config.ConfigFactory
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.SortedSet

class BundleEmitterSpec extends FlatSpec with Matchers {
  private val packageName = "org.example.i18n"

  "A Bundle Emitter" should "emit the correct package" in {
    val expected =
      """package """ + packageName + """
        |
        |object Bundle {""".stripMargin

    BundleEmitter(ConfigFactory.empty(), packageName).emit() should startWith(expected)
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

    BundleEmitter(config, packageName).languages should be(Set("fr", "de"))
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
    BundleEmitter(config, packageName).translationKeys should be(Set("text", "topic.key1", "text2"))
  }

  it must "identify all the path keys" in {
    BundleEmitter.pathKeys(Set("text", "topic.key1", "text2")) should be(Set("topic"))
  }

  it should "split the keys" in {
    BundleEmitter.splitKeys(Set("text", "topic.key1", "text2")) should be(
      Set("topic.key1") -> Set("text", "text2"))
  }

  it should "build the tree" in {
    val configString =
      """
        |fr {
        |    text = Bonjour
        |    text3 = "Mon paramètre est {0}"
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
    import BundleEmitter._
    val config = ConfigFactory.parseString(configString.stripMargin)
    BundleEmitter(config, packageName).buildTree() should be(
      new Root(
        SortedSet(
          Branch("topic", SortedSet(SimpleMessage("key1", Map("fr" -> "Salade")))),
          SimpleMessage("text", Map("fr"  -> "Bonjour", "de" -> "Guttentag")),
          SimpleMessage("text2", Map("de" -> "ich heiße MARVIN")),
          ParametrizedMessage("text3",
                              Map("fr" -> "Mon paramètre est {0}"),
                              List(Param.StringParam))
        )
      ))
  }

  it must "emit the structure" in {
    val configString =
      """
        |fr {
        |    text = Bonjour
        |    text3 = "Mon paramètre est {0}"
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

    val expected =
      """abstract class I18N {
        |def text: String
        |def text2: String
        |def text3(x0: String): String
        |abstract class Topic {
        |def key1: String
        |}
        |
        |def topic: Topic
        |
        |}""".stripMargin

    val config = ConfigFactory.parseString(configString.stripMargin)
    BundleEmitter(config, packageName).emitStructure() should be(expected)
  }
  it must "emit the values" in {
    import BundleEmitter.quote
    val configString =
      """
        |fr {
        |    text = Bonjour
        |    text3 = "Mon paramètre est {0}"
        |    topic {
        |      key1 = Salade
        |    }
        |}
        |
        |de {
        |    text  = 1
        |    text2 = ich heiße MARVIN
        |}
        |""".stripMargin

    val config = ConfigFactory.parseString(configString.stripMargin)
    BundleEmitter(config, packageName).emitValues("fr") should be(
      s"""object fr extends I18N {
         |val text= ${quote("Bonjour")}
         |val text2= ${quote("??? fr.text2 ???")}
         |def text3(x0: String): String= java.text.MessageFormat.format(${quote(
           "Mon paramètre est {0}")}, x0)
         |object topic extends  Topic {
         |val key1= ${quote("Salade")}
         |}
         |
        |}""".stripMargin)

    BundleEmitter(config, packageName).emitValues("de") should be(s"""object de extends I18N {
         |val text= ${quote("1")}
         |val text2= ${quote("ich heiße MARVIN")}
         |def text3(x0: String): String= java.text.MessageFormat.format(${quote("??? de.text3 ???")}, x0)
         |object topic extends  Topic {
         |val key1= ${quote("??? de.topic.key1 ???")}
         |}
         |
        |}""".stripMargin)

  }
}
