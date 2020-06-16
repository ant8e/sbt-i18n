package tech.ant8e.sbt.i18n

import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import tech.ant8e.sbt.i18n.BundleEmitter.Param._

class FormatSpec extends AnyFlatSpec with Matchers with EitherValues {

  "A Param parser" should "identity the correct param type " in {
    identifyParams("").right.value should be(empty)
    identifyParams("{0}").right.value should be(List(StringParam))
    identifyParams("{4}").right.value should be(List.fill(5)(StringParam))

    identifyParams("{0,number}").right.value should be(List(DoubleParam))
    identifyParams("{0,number,integer}").right.value should be(List(LongParam))
    identifyParams("{0,number,currency}").right.value should be(List(DoubleParam))
    identifyParams("{0,number,percent}").right.value should be(List(DoubleParam))
    identifyParams("{0,number,##0.#####E0}").right.value should be(List(DoubleParam))

    identifyParams("{0,date}").right.value should be(List(DateParam))
    identifyParams("{0,date,short}").right.value should be(List(DateParam))
    identifyParams("{0,date,medium}").right.value should be(List(DateParam))
    identifyParams("{0,date,long}").right.value should be(List(DateParam))
    identifyParams("{0,date,full}").right.value should be(List(DateParam))
    identifyParams("{0,date,yyyyy.MMMMM.dd GGG hh:mm aaa}").right.value should be(List(DateParam))

    identifyParams("{0,time}").right.value should be(List(DateParam))
    identifyParams("{0,time,short}").right.value should be(List(DateParam))
    identifyParams("{0,time,medium}").right.value should be(List(DateParam))
    identifyParams("{0,time,long}").right.value should be(List(DateParam))
    identifyParams("{0,time,full}").right.value should be(List(DateParam))
    identifyParams("{0,time,yyyyy.MMMMM.dd GGG hh:mm aaa}").right.value should be(List(DateParam))

    identifyParams(
      "{0,choice,-1#is negative| 0#is zero or fraction | 1#is one |1.0<is 1+ |2#is two |2<is more than 2.}"
    ).right.value should be(List(DoubleParam))
  }

  it should "not choke on invalid format" in {
    identifyParams("{0,whatever}") should be('left)
    identifyParams("{0") should be('left)
  }
}
