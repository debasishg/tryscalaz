package net.debasishg.domain.trade.dsl

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class TradeModelSpec extends Spec with ShouldMatchers {

  import scalaz._
  import Scalaz._

  val t1 = Map("account" -> "a-123", "instrument" -> "google", "refNo" -> "r-123", "market" -> "HongKong", "unitPrice" -> "12.25", "quantity" -> "200")
  val t2 = Map("account" -> "b-123", "instrument" -> "ibm", "refNo" -> "r-234", "market" -> "Singapore", "unitPrice" -> "15.25", "quantity" -> "400")

  describe("trades") {
    it("should create and operate on multiple trades") {
      import Trades._

      val trd1 = makeTrade(t1)
      val trd2 = makeTrade(t2)

      (trd1 ∘ lifecycle) should equal(Some(Some(3307.5000)))
      (List(trd1, trd2) ∘∘ lifecycle) should equal (List(Some(Some(3307.5000)), Some(Some(8845.0000))))
    }
  }
}
