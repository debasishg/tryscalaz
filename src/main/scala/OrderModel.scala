package net.debasishg.domain.trade.dsl

import java.util.{Date, Calendar}
import scalaz._
import Scalaz._

trait OrderModel {this: RefModel =>
  case class LineItem(ins: Instrument, qty: BigDecimal, price: BigDecimal)
  case class Order(no: String, date: Date, customer: Customer, items: List[LineItem])

  implicit def BigDecimalSemigroup: Semigroup[BigDecimal] = semigroup(_ + _)
  type QtyPrice = (BigDecimal, BigDecimal)

  implicit def QtyPriceSemigroup: Semigroup[QtyPrice] = semigroup {
    (qp1, qp2) => {
      val (qty1, price1) = qp1
      val (qty2, price2) = qp2
      (qty1 + qty2, (qty1 * price1 + qty2 * price2) / (qty1 + qty2))
    }
  }

  implicit def OrderSemigroup =
    new Semigroup[Order] {
      def append(o1: Order, o2: => Order) = {
        val Order(no1, dt1, c1, is1) = o1
        val Order(no2, dt2, c2, is2) = o2
        val items =
          (is1 ++ is2).foldLeft(Map.empty[Instrument, QtyPrice])((a, i) => 
            a + (i.ins -> a.get(i.ins).map(_ |+| (i.qty, i.price)).getOrElse((i.qty, i.price))))
        val lineItems = items.map{ case(k, v) => LineItem(k, v._1, v._2) }
        Order(no1 |+| no2, dt1, c1, lineItems.toList)
      }
    }

  def fromClientOrders(cos: List[Map[String, String]]) = {
    cos map {co =>
      val ins = co("instrument").split("-")
      val lineItems = ins map {in =>
        val arr = in.split("/")
        LineItem(arr(0), BigDecimal(arr(1)), BigDecimal(arr(2)))
      }
      Order(co("no"), Calendar.getInstance.getTime, co("customer"), lineItems.toList)
    }
  }

  def validItems(items: List[LineItem]): Validation[String, List[LineItem]] = {
    if (items.isEmpty) "Cannot have an empty list of line items for order".fail
    else items.success
  }

  def validDate(date: Date): Validation[String, Date] = {
    date.success
  }

  // using Validation as an applicative
  // can be combined to accumulate exceptions
  def makeOrder(no: String, date: Date, customer: Customer, items: List[LineItem]) =
    (validItems(items).liftFailNel |@|
      validDate(date).liftFailNel) { (i, d) => Order(no, d, customer, i) }
}
