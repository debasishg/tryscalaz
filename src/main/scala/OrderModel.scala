package net.debasishg.domain.trade.dsl

/**
 * Created by IntelliJ IDEA.
 * User: debasish
 * Date: 24/12/10
 * Time: 10:36 PM
 * To change this template use File | Settings | File Templates.
 */

trait OrderModel {this: RefModel => 
  case class LineItem(ins: Instrument, qty: BigDecimal, price: BigDecimal)
  case class Order(no: String, date: java.util.Date, items: List[LineItem])
}
