package tech.ant8e.sbt.i18n

import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import tech.ant8e.sbt.i18n.BundleEmitter.Param
import tech.ant8e.sbt.i18n.BundleEmitter.Param._
import tech.ant8e.sbt.i18n.BundleEmitter.ParamsExtractor

class ParamsExtractorSpec
    extends AnyFlatSpec
    with Matchers
    with EitherValues
    with TableDrivenPropertyChecks {

  "A Params extractor" should "extract the correct params" in {
    val table = Table(
      ("pattern", "expected"),
      ("", List.empty),
      ("{0}", List(Param("0", AnyParam))),
      ("{4}", List(Param("4", AnyParam))),
      ("{0,number}", List(Param("0", DoubleParam))),
      ("{0,number,integer}", List(Param("0", LongParam))),
      ("{0,number,currency}", List(Param("0", DoubleParam))),
      ("{0,number,percent}", List(Param("0", DoubleParam))),
      ("{0,number,##0.#####E0}", List(Param("0", DoubleParam))),
      ("{0,date}", List(Param("0", DateParam))),
      ("{0,date,short}", List(Param("0", DateParam))),
      ("{0,date,medium}", List(Param("0", DateParam))),
      ("{0,date,long}", List(Param("0", DateParam))),
      ("{0,date,full}", List(Param("0", DateParam))),
      ("{0,date,yyyyy.MMMMM.dd GGG hh:mm aaa}", List(Param("0", DateParam))),
      ("{0,time}", List(Param("0", DateParam))),
      ("{0,time,short}", List(Param("0", DateParam))),
      ("{0,time,medium}", List(Param("0", DateParam))),
      ("{0,time,long}", List(Param("0", DateParam))),
      ("{0,time,full}", List(Param("0", DateParam))),
      ("{0,time,yyyyy.MMMMM.dd GGG hh:mm aaa}", List(Param("0", DateParam))),
      (
        "{0,choice,-1#is negative| 0#is zero or fraction | 1#is one |1.0<is 1+ |2#is two |2<is more than 2.}",
        List(Param("0", LongParam))
      ),
      (
        "{0, plural, one {One dog is} other {# dogs are}}",
        List(Param("0", LongParam))
      ),
      (
        "{0, selectordinal, one {#st} two {#nd} few {#rd} other {#th}}",
        List(Param("0", LongParam))
      ),
      (
        "{0, select, male {He uses} female {She uses} other {They use}}",
        List(Param("0", EnumParam("0", List("male", "female", "other"))))
      ),
      ("", List.empty),
      ("{i}", List(Param("i", AnyParam))),
      ("{count,number}", List(Param("count", DoubleParam))),
      ("{n,number,integer}", List(Param("n", LongParam))),
      ("{spent,number,currency}", List(Param("spent", DoubleParam))),
      ("{fraction,number,percent}", List(Param("fraction", DoubleParam))),
      ("{d,number,##0.#####E0}", List(Param("d", DoubleParam))),
      ("{d,date}", List(Param("d", DateParam))),
      ("{day,date,short}", List(Param("day", DateParam))),
      ("{day,date,medium}", List(Param("day", DateParam))),
      ("{day,date,long}", List(Param("day", DateParam))),
      ("{day,date,full}", List(Param("day", DateParam))),
      ("{time,date,yyyyy.MMMMM.dd GGG hh:mm aaa}", List(Param("time", DateParam))),
      ("{t,time}", List(Param("t", DateParam))),
      ("{at,time,short}", List(Param("at", DateParam))),
      ("{at,time,medium}", List(Param("at", DateParam))),
      ("{at,time,long}", List(Param("at", DateParam))),
      ("{at,time,full}", List(Param("at", DateParam))),
      ("{at,time,yyyyy.MMMMM.dd GGG hh:mm aaa}", List(Param("at", DateParam))),
      (
        "{n,choice,-1#is negative| 0#is zero or fraction | 1#is one |1.0<is 1+ |2#is two |2<is more than 2.}",
        List(Param("n", LongParam))
      ),
      (
        "{dogsCount, plural, one {One dog is} other {# dogs are}}",
        List(Param("dogsCount", LongParam))
      ),
      (
        "{year, selectordinal, one {#st} two {#nd} few {#rd} other {#th}}",
        List(Param("year", LongParam))
      ),
      (
        "{gender, select, male {He uses} female {She uses} other {They use}}",
        List(Param("gender", EnumParam("gender", List("male", "female", "other"))))
      )
    )

    forAll(table) { (pattern, expected) =>
      ParamsExtractor.extractParams(pattern).right.value should be(expected)
    }
  }

  it should "not choke on invalid format" in {
    ParamsExtractor.extractParams("{0,whatever}") should be('left)
    ParamsExtractor.extractParams("{0") should be('left)
  }
}
