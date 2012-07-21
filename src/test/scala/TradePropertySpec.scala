package net.debasishg.domain.trade
package model

import org.scalatest.PropSpec
import org.scalatest.prop.{PropertyChecks, Checkers}
import org.scalatest.matchers.ShouldMatchers

import scalaz._
import Scalaz._

import org.scalacheck.Arbitrary._
import org.scalacheck._
import Prop.forAll
import Gen._
import Arbitrary.arbitrary

import TradeModel._
import dsl.TradeDsl._

object TradeGen {
  implicit lazy val arbMarket: Arbitrary[Market] = 
    Arbitrary(oneOf(value(HongKong), value(Singapore), value(Tokyo), value(NewYork), value(Other)))

  implicit lazy val arbTrade: Arbitrary[Trade] =
    Arbitrary {
      for {
        a <- Gen.oneOf("acc-01", "acc-02", "acc-03", "acc-04") 
        i <- Gen.oneOf("ins-01", "ins-02", "ins-03", "ins-04") 
        r <- Gen.oneOf("r-001", "r-002", "r-003")
        m <- arbitrary[Market]
        u <- Gen.oneOf(BigDecimal(1.5), BigDecimal(2), BigDecimal(10)) 
        q <- Gen.oneOf(BigDecimal(100), BigDecimal(200), BigDecimal(300)) 
      } yield Trade(a, i, r, m, u, q)
    }

  implicit lazy val arbClientOrder: Arbitrary[ClientOrder] =
    Arbitrary {
      for { 
        ono <- arbitrary[Int]
        cust <- Gen.oneOf("nomura", "chase", "meryll")
        qty <- Gen.choose(50, 1000)
        unit <- Gen.choose(10, 40)
        instruments <- Gen.containerOfN[Set, Instrument](3, Gen.oneOf("google", "ibm", "oracle", "cisco"))
      } yield Map("no"          -> ("o-" + String.valueOf(ono)),
                  "customer"    -> cust,
                  "instrument"  -> instruments.map(_ + "/" + qty + "/" + unit).mkString("-"))
    }

  implicit lazy val arbTradeGenArgs: Arbitrary[(Market, Account, List[Account])] =
    Arbitrary {
      for {
        m <- arbitrary[Market]
        b <- Gen.oneOf("b-acc-01", "b-acc-02", "b-acc-03", "b-acc-04") suchThat(_.length > 0) 
        c <- Gen.containerOfN[List, Account](2, Gen.oneOf("acc-01", "acc-02", "acc-03", "acc-04"))
      } yield ((m, b, c))
    }
}

import TradeGen._
class TradeSpecification extends PropSpec with PropertyChecks with ShouldMatchers {
  property("enrichment should result in netvalue > 0") {
    forAll((a: Trade) =>
      enrichTrade(a).netAmount.get should be > (BigDecimal(0)))
  }

  property("enrichment should mean netValue equals principal + taxes") {
    forAll((a: Trade) => { 
      val et = enrichTrade(a)
      et.netAmount should equal (et.taxFees.map(_.foldLeft(principal(et))((a, b) => a + b._2)))
    })
  }

  property("client trade allocation in the trade pipeline should maintain quantity invariant") {
    forAll { (clientOrders: List[ClientOrder], args: (Market, Account, List[Account])) =>
      whenever (clientOrders.size > 0 && args._2.size > 0 && args._3.size > 0) {
        val trades = tradeGeneration(args._1, args._2, args._3)(clientOrders)
        trades.size should be > 0
        (trades.sequence[({type λ[α]=Validation[NonEmptyList[String],α]})#λ, Trade]) match {
          case Success(l) => {
            val tradeQuantity = l.map(_.quantity).sum 
            val orderQuantity = fromClientOrders(clientOrders).map(_.items).flatten.map(_.qty).sum 
            tradeQuantity should equal(orderQuantity)
          }
          case _ => fail("should get a list of size > 0")
        }
      }
    }
  }
} 
