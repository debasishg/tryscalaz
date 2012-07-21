package net.debasishg.domain.trade
package dsl

/**
 * Created by IntelliJ IDEA.
 * User: debasish
 * Date: 23/12/10
 * Time: 10:53 PM
 * To change this template use File | Settings | File Templates.
 */

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class TradeDslSpec extends Spec with ShouldMatchers {

  import scalaz._
  import Scalaz._

  describe("trade lifecycle") {
    it("should create and operate on multiple trades") {
      import model.TradeModel._
      import TradeDsl._

      val trd1 = makeTrade("a-123", "google", "r-123", HongKong, 12.25, 200)
      val trd2 = makeTrade("a-125", "ibm", "r-125", Tokyo, 22.25, 250)
      val trd3 = makeTrade("a-126", "cisco", "r-126", NewYork, 210, 600)   // not valid

      (trd1 ∘ enrich) should equal(Success(Some(3307.5000)))
      (Seq(trd1, trd2).sequence[({type λ[α]=Validation[NonEmptyList[String],α]})#λ, Trade] ∘∘ enrich) should equal(Success(List(Some(3307.5000), Some(7509.3750))))

      val l = (Seq(trd1, trd2, trd3).sequence[({type λ[α]=Validation[NonEmptyList[String],α]})#λ, Trade] ∘∘ enrich)
      l match {
        case Failure(ll) => ll.list should equal(List("price must be <= 100", "qty must be <= 500"))
        case _ => fail("must give a Failure")
      }
    }
  }

  describe("order-execute-allocate") {
    it("should execute in pipeline") {
      import model.TradeModel._
      import TradeDsl._

      val clientOrders = List(
        Map("no" -> "o-123", "customer" -> "chase", "instrument" -> "goog/100/30-ibm/200/12"),
        Map("no" -> "o-124", "customer" -> "nomura", "instrument" -> "cisco/100/30-oracle/200/12")
      )

      val trades = tradeGeneration(NewYork, "b-123", List("c1-123", "c2-123"))(clientOrders)
      trades.size should equal(8)
    }
  }

  describe("order-execute-allocate and enrich in pipeline") {
    it("should execute in pipeline") {
      import model.TradeModel._
      import TradeDsl._

      val clientOrders = List(
        Map("no" -> "o-123", "customer" -> "chase", "instrument" -> "goog/100/30-ibm/200/12"),
        Map("no" -> "o-124", "customer" -> "nomura", "instrument" -> "cisco/100/30-oracle/200/12")
      )

      val trades = tradeGeneration(NewYork, "b-123", List("c1-123", "c2-123"))(clientOrders)
      (trades.sequence[({type λ[α]=Validation[NonEmptyList[String],α]})#λ, Trade] ∘∘ enrich) match {
        case Success(l) => l.size should equal(8)
        case _ => fail("should get a list of size 8")
      }
    }
  }

  import java.util.{Date, Calendar}
  def addDays(dt: Date, no: Int) = {
    val c = Calendar.getInstance
    c.setTime(dt)
    c.add(Calendar.DATE, no)
    c.getTime
  }

  describe("trade lens composition") {
    it("should compose") {
      import model.TradeModel._
      import TradeDsl._

      val trd1 = Trade("a-123", "google", "r-123", HongKong, 12.25, 200)
      val trd2 = Trade("a-125", "ibm", "r-125", Tokyo, 22.25, 250)
      val trd3 = Trade("a-126", "cisco", "r-126", NewYork, 20, 200)

      val valueDate = Calendar.getInstance.getTime

      // map across the list functor 
      val ts = Seq(trd1, trd2, trd3) ∘ (valueDateLens.set(_, Some(valueDate))) 
      println(ts)

      // map across the list functor 
      val valueDates = ts ∘ (_.valueDate)

      val now = Calendar.getInstance.getTime
      val valueDateProcessor: Trade => Trade = {t =>
        valueDateLens.set(t, 
          t.valueDate map (addDays(_, 3)) orElse Some(addDays(now, 3)))
      }
      
      val taxFeeProcessor: Trade => Trade = {t =>
        // taxFeeLens.set(trade, taxes(trade))
        t
      }

      val netAmountProcessor: Trade => Trade = {t =>
        netAmountLens.set(t, t.taxFees.map(_.foldl(principal(t))((a, b) => a + b._2)))
      }

      // map across the Function1 functor 
      // val trade = (valueDateProcessor ∘ taxFeeProcessor ∘ netAmountProcessor) apply trd1 
      // println(trade)

      // map across an option functor
      val t = some(trd1) ∘ valueDateProcessor
      println(t)

      
    }
  }
}
