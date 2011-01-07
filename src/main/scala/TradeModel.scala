package net.debasishg.domain.trade.dsl

/**
 * Created by IntelliJ IDEA.
 * User: debasish
 * Date: 23/12/10
 * Time: 6:06 PM
 * To change this template use File | Settings | File Templates.
 */

import scalaz._
import Scalaz._

trait TradeModel {this: RefModel =>

  // the main domain class
  case class Trade(account: Account, instrument: Instrument, refNo: String, market: Market,
    unitPrice: BigDecimal, quantity: BigDecimal)

  // various tax/fees to be paid when u do a trade
  sealed trait TaxFeeId
  case object TradeTax extends TaxFeeId
  case object Commission extends TaxFeeId
  case object VAT extends TaxFeeId

  // rates of tax/fees expressed as fractions of the principal of the trade
  val rates: Map[TaxFeeId, BigDecimal] = Map(TradeTax -> 0.2, Commission -> 0.15, VAT -> 0.1)

  // tax and fees applicable for each market
  // Other signifies the general rule
  val taxFeeForMarket: Map[Market, List[TaxFeeId]] = Map(Other -> List(TradeTax, Commission), Singapore -> List(TradeTax, Commission, VAT))

  // get the list of tax/fees applicable for this trade
  // depends on the market
  val forTrade: Trade => Option[List[TaxFeeId]] = {trade =>
    taxFeeForMarket.get(trade.market) <+> taxFeeForMarket.get(Other)
  }

  def principal(trade: Trade) = trade.unitPrice * trade.quantity
  // combinator to value a tax/fee for a specific trade
  def valueAs(trade: Trade, tid: TaxFeeId) = ((rates get tid) map (_ * principal(trade))) getOrElse (BigDecimal(0))

  // all tax/fees for a specific trade
  val taxFeeCalculate: Trade => List[TaxFeeId] => List[(TaxFeeId, BigDecimal)] = {t => {tids =>
    tids.zip(tids.map(valueAs(t, _)))
  }}

  // validate quantity
  def validQuantity(qty: BigDecimal): Validation[String, BigDecimal] =
    if (qty <= 0) "qty must be > 0".fail
    else if (qty > 500) "qty must be <= 500".fail
    else qty.success

  // validate unit price
  def validUnitPrice(price: BigDecimal): Validation[String, BigDecimal] =
    if (price <= 0) "price must be > 0".fail
    else if (price > 100) "price must be <= 100".fail
    else price.success

  // using Validation as an applicative
  // can be combined to accumulate exceptions
  def makeTrade(account: Account, instrument: Instrument, refNo: String, market: Market,
    unitPrice: BigDecimal, quantity: BigDecimal) =
    (validUnitPrice(unitPrice).liftFailNel |@|
      validQuantity(quantity).liftFailNel) { (u, q) => Trade(account, instrument, refNo, market, u, q) }

  val enrichTradeWith: Trade => List[(TaxFeeId, BigDecimal)] => BigDecimal = {trade => {taxes =>
    taxes.foldLeft(principal(trade))((a, b) => a + b._2)
  }}
}

object TradeModel extends TradeModel with ExecutionModel with OrderModel with RefModel with ContractNoteModel