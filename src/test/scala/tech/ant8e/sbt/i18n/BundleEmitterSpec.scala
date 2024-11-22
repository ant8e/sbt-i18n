package tech.ant8e.sbt.i18n

import com.typesafe.config.ConfigFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BundleEmitterSpec extends AnyFlatSpec with Matchers {
  private val packageName = "org.example.i18n"

  "A Bundle Emitter" should "emit the correct package" in {
    val expected =
      """package """ + packageName + """
        |
        |import""".stripMargin

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

  it must "emit the languages map" in {
    val configString =
      """
      |fr {
      |    text = Bonjour
      |}
      |de {
      |    text = Guttentag
      |}
      |zh-Hans {
      |    text = 你好
      |}
      |""".stripMargin

    val config   = ConfigFactory.parseString(configString)
    val expected =
      """object Bundle {
        | val languages: Map[String, I18N] = Map(("de", de), ("zh-Hans", `zh-Hans`), ("fr", fr))""".stripMargin

    BundleEmitter(config, packageName).emit() should include(expected)
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
    val config       = ConfigFactory.parseString(configString.stripMargin)
    BundleEmitter(config, packageName).translationKeys should be(Set("text", "topic.key1", "text2"))
  }

  it must "identify all the path keys" in {
    BundleEmitter.pathKeys(Set("text", "topic.key1", "text2")) should be(Set("topic"))
  }

  it should "split the keys" in {
    BundleEmitter.splitKeys(Set("text", "topic.key1", "text2")) should be(
      Set("topic.key1") -> Set("text", "text2")
    )
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
        |    topic.key2 = Légumes
        |}
        |
        |de {
        |    text  = Guttentag
        |    text2 = ich heiße MARVIN
        |}
        |""".stripMargin
    import BundleEmitter._
    val config       = ConfigFactory.parseString(configString.stripMargin)
    val root         = BundleEmitter(config, packageName).buildTree()
    val expected     = new Root(
      Set(
        Branch(
          "topic",
          Set(
            SimpleMessage("key1", Map("fr" -> "Salade")),
            SimpleMessage("key2", Map("fr" -> "Légumes"))
          )
        ),
        SimpleMessage("text", Map("fr" -> "Bonjour", "de" -> "Guttentag")),
        SimpleMessage("text2", Map("de" -> "ich heiße MARVIN")),
        ParametrizedMessage(
          "text3",
          Map("fr" -> "Mon paramètre est {0}"),
          List(Param("0", Param.AnyParam))
        )
      )
    )

    root should be(expected)
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
        |    now = "Jetzt ist es {0,time,short} Uhr"
        |}
        |""".stripMargin

    val expected =
      """abstract class I18N {
        |protected val locale: Locale
        |def now(x0: ZonedDateTime): String
        |def text: String
        |def text2: String
        |def text3(x0: Any): String
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
        |    topic.key2 = Légumes
        |}
        |
        |de {
        |    text  = 1
        |    text2 = ich heiße MARVIN
        |    now = "Jetzt ist es {0,time,short} Uhr"
        |    topic {
        |      key1 = Salat
        |    }
        |}
        |""".stripMargin

    val config = ConfigFactory.parseString(configString.stripMargin)
    BundleEmitter(config, packageName).emitValues("fr") should be(
      s"""object fr extends I18N {
         |override protected val locale: Locale = Locale.forLanguageTag("fr")
         |
         |def now(x0: ZonedDateTime): String = new MessageFormat(${quote(
          "??? fr.now ???"
        )}, locale).format(java.util.Map.of("0", toCalendar(x0)))
         |val text= ${quote("Bonjour")}
         |val text2= ${quote("??? fr.text2 ???")}
         |def text3(x0: Any): String = new MessageFormat(${quote(
          "Mon paramètre est {0}"
        )}, locale).format(java.util.Map.of("0", x0))
         |object topic extends  Topic {
         |val key1= ${quote("Salade")}
         |val key2= ${quote("Légumes")}
         |}
         |
        |}""".stripMargin
    )

    val emitter: BundleEmitter = BundleEmitter(config, packageName)
    emitter.emitValues("de") should be(s"""object de extends I18N {
         |override protected val locale: Locale = Locale.forLanguageTag("de")
         |
         |def now(x0: ZonedDateTime): String = new MessageFormat(${quote(
                                           "Jetzt ist es {0,time,short} Uhr"
                                         )}, locale).format(java.util.Map.of("0", toCalendar(x0)))
         |val text= ${quote("1")}
         |val text2= ${quote("ich heiße MARVIN")}
         |def text3(x0: Any): String = new MessageFormat(${quote(
                                           "??? de.text3 ???"
                                         )}, locale).format(java.util.Map.of("0", x0))
         |object topic extends  Topic {
         |val key1= ${quote("Salat")}
         |val key2= ${quote("??? de.topic.key2 ???")}
         |}
         |
        |}""".stripMargin)

  }
  it must "break emit the values when breakOnMissingKeys=true" in {
    val configString =
      """
        |fr {
        |    topic {
        |      key1 = Salade
        |    }
        |}
        |
        |de {
        |    topic {
        |    }
        |}
        |""".stripMargin

    val config = ConfigFactory.parseString(configString.stripMargin)
    val caught =
      intercept[Exception] {
        BundleEmitter(config, packageName, breakOnMissingKeys = true).emit()
      }
    caught.getMessage shouldBe "There's missing value for 'de.topic.key1'"
  }
  it must "emit the values with merged parameters" in {
    import BundleEmitter.quote
    val configString =
      """
        |fr {
        |    text1 = "Mon paramètre est {0}"
        |    text2 = "Mon paramètre est vide"
        |}
        |
        |de {
        |    text1 = "Meine Einstellung ist leer"
        |    text2 = "Meine Einstellung ist {0}"
        |}
        |""".stripMargin

    val config                 = ConfigFactory.parseString(configString.stripMargin)
    val emitter: BundleEmitter = BundleEmitter(config, packageName)
    emitter.emitValues("fr") should be(s"""object fr extends I18N {
                                          |override protected val locale: Locale = Locale.forLanguageTag("fr")
                                          |
                                          |def text1(x0: Any): String = new MessageFormat(${quote(
                                           "Mon paramètre est {0}"
                                         )}, locale).format(java.util.Map.of("0", x0))
                                          |def text2(x0: Any): String = new MessageFormat(${quote(
                                           "Mon paramètre est vide"
                                         )}, locale).format(java.util.Map.of("0", x0))
                                          |}""".stripMargin)
    emitter.emitValues("de") should be(s"""object de extends I18N {
                                          |override protected val locale: Locale = Locale.forLanguageTag("de")
                                          |
                                          |def text1(x0: Any): String = new MessageFormat(${quote(
                                           "Meine Einstellung ist leer"
                                         )}, locale).format(java.util.Map.of("0", x0))
                                          |def text2(x0: Any): String = new MessageFormat(${quote(
                                           "Meine Einstellung ist {0}"
                                         )}, locale).format(java.util.Map.of("0", x0))
                                          |}""".stripMargin)
  }

  it should "build the tree ICU" in {
    val configString =
      """
        |en {
        |    choice = "{n,choice,-1#is negative| 0#is zero or fraction | 1#is one |1.0<is 1+ |2#is two |2<is more than 2.}"
        |    plural = "{dogsCount, plural, one {One dog is} other {# dogs are}}"
        |    selectordinal = "{year, selectordinal, one {#st} two {#nd} few {#rd} other {#th}}"
        |    select = "{gender, select, male {He uses} female {She uses} other {They use}}"
        |    multi = "{n,number,currency} {year, selectordinal, one {#st} two {#nd} three {#rd} other {#th}} {0}"
        |}
        |""".stripMargin
    import BundleEmitter._
    val config       = ConfigFactory.parseString(configString.stripMargin)
    val root         = BundleEmitter(config, packageName).buildTree()
    val expected     = new Root(
      Set(
        ParametrizedMessage(
          "choice",
          Map(
            "en" -> "{n,choice,-1#is negative| 0#is zero or fraction | 1#is one |1.0<is 1+ |2#is two |2<is more than 2.}"
          ),
          List(Param("n", Param.LongParam))
        ),
        ParametrizedMessage(
          "multi",
          Map(
            "en" -> "{n,number,currency} {year, selectordinal, one {#st} two {#nd} three {#rd} other {#th}} {0}"
          ),
          List(
            Param("n", Param.DoubleParam),
            Param("year", Param.LongParam),
            Param("0", Param.AnyParam)
          )
        ),
        ParametrizedMessage(
          "plural",
          Map(
            "en" -> "{dogsCount, plural, one {One dog is} other {# dogs are}}"
          ),
          List(Param("dogsCount", Param.LongParam))
        ),
        ParametrizedMessage(
          "selectordinal",
          Map(
            "en" -> "{year, selectordinal, one {#st} two {#nd} few {#rd} other {#th}}"
          ),
          List(Param("year", Param.LongParam))
        ),
        ParametrizedMessage(
          "select",
          Map(
            "en" -> "{gender, select, male {He uses} female {She uses} other {They use}}"
          ),
          List(Param("gender", Param.EnumParam("gender", List("male", "female", "other"))))
        )
      )
    )

    root should be(expected)
  }

  it must "emit the structure ICU" in {
    val configString =
      """
        |en {
        |    choice = "{n,choice,-1#is negative| 0#is zero or fraction | 1#is one |1.0<is 1+ |2#is two |2<is more than 2.}"
        |    plural = "{dogsCount, plural, one {One dog is} other {# dogs are}}"
        |    selectordinal = "{year, selectordinal, one {#st} two {#nd} few {#rd} other {#th}}"
        |    select = "{gender, select, male {He uses} female {She uses} other {They use}}"
        |    multi = "{n,number,currency} {year, selectordinal, one {#st} two {#nd} three {#rd} other {#th}} {0}"
        |}
        |""".stripMargin

    val expected =
      """abstract class I18N {
        |protected val locale: Locale
        |def choice(n: Long): String
        |def multi(n: Double, year: Long, x0: Any): String
        |def plural(dogsCount: Long): String
        |sealed trait Gender
        |object Gender {
        |case object male extends Gender
        |case object female extends Gender
        |case object other extends Gender
        |}
        |def select(gender: Gender): String
        |def selectordinal(year: Long): String
        |}""".stripMargin

    val config = ConfigFactory.parseString(configString.stripMargin)
    BundleEmitter(config, packageName).emitStructure() should be(expected)
  }
  it must "emit the values ICU" in {
    import BundleEmitter.quote
    val configString =
      """
        |en {
        |    choice = "{n,choice,-1#is negative| 0#is zero or fraction | 1#is one |1.0<is 1+ |2#is two |2<is more than 2.}"
        |    plural = "{dogsCount, plural, one {One dog is} other {# dogs are}}"
        |    selectordinal = "{year, selectordinal, one {#st} two {#nd} few {#rd} other {#th}}"
        |    select = "{gender, select, male {He uses} female {She uses} other {They use}}"
        |    multi = "{n,number,currency} {year, selectordinal, one {#st} two {#nd} three {#rd} other {#th}} {0}"
        |}
        |""".stripMargin

    val config   = ConfigFactory.parseString(configString.stripMargin)
    val expected =
      s"""object en extends I18N {
        |override protected val locale: Locale = Locale.forLanguageTag("en")
        |
        |def choice(n: Long): String = new MessageFormat(${quote(
          "{n,choice,-1#is negative| 0#is zero or fraction | 1#is one |1.0<is 1+ |2#is two |2<is more than 2.}"
        )}, locale).format(java.util.Map.of("n", n))
        |def multi(n: Double, year: Long, x0: Any): String = new MessageFormat(${quote(
          "{n,number,currency} {year, selectordinal, one {#st} two {#nd} three {#rd} other {#th}} {0}"
        )}, locale).format(java.util.Map.of("n", n, "year", year, "0", x0))
        |def plural(dogsCount: Long): String = new MessageFormat(${quote(
          "{dogsCount, plural, one {One dog is} other {# dogs are}}"
        )}, locale).format(java.util.Map.of("dogsCount", dogsCount))
        |def select(gender: Gender): String = new MessageFormat(${quote(
          "{gender, select, male {He uses} female {She uses} other {They use}}"
        )}, locale).format(java.util.Map.of("gender", gender))
        |def selectordinal(year: Long): String = new MessageFormat(${quote(
          "{year, selectordinal, one {#st} two {#nd} few {#rd} other {#th}}"
        )}, locale).format(java.util.Map.of("year", year))
        |}""".stripMargin
    BundleEmitter(config, packageName).emitValues("en") should be(expected)

  }
}
