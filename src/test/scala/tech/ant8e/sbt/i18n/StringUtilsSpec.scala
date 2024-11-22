package tech.ant8e.sbt.i18n

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import tech.ant8e.sbt.i18n.BundleEmitter.StringUtils

class StringUtilsSpec extends AnyFlatSpec with Matchers {
  "A String util" should "build type name" in {
    StringUtils.typeName("armyVote") shouldBe "ArmyVote"
    StringUtils.typeName("profit_fund") shouldBe "ProfitFund"
    StringUtils.typeName("MonthlyIncome") shouldBe "MonthlyIncome"
    StringUtils.typeName("Deny_Sale") shouldBe "DenySale"
  }

  it should "build field name" in {
    StringUtils.fieldName("armyVote") shouldBe "armyVote"
    StringUtils.fieldName("profit_fund") shouldBe "profitFund"
    StringUtils.fieldName("MonthlyIncome") shouldBe "monthlyIncome"
    StringUtils.fieldName("Deny_Sale") shouldBe "denySale"
  }

  it should "detect all digit string" in {
    StringUtils.isNumeric("1234") shouldBe true
    StringUtils.isNumeric("a123") shouldBe false
    StringUtils.isNumeric("123b") shouldBe false
    StringUtils.isNumeric("123.12") shouldBe false
  }
}
