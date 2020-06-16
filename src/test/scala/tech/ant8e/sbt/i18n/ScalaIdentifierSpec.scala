package tech.ant8e.sbt.i18n

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ScalaIdentifierSpec extends AnyFlatSpec with Matchers {

  "An Identifier" should "emit scala identifier" in {

    ScalaIdentifier.asIdentifier("plain") should be("plain")
    ScalaIdentifier.asIdentifier("") should be("")
    ScalaIdentifier.asIdentifier("plain_with_separation") should be("plain_with_separation")
    ScalaIdentifier.asIdentifier("*") should be("`*`")
    ScalaIdentifier.asIdentifier("le brie sent fort") should be("`le brie sent fort`")
    ScalaIdentifier.asIdentifier("package") should be("`package`")
    ScalaIdentifier.asIdentifier("class") should be("`class`")
  }
}
