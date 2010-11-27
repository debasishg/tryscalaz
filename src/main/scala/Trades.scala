package net.debasishg.domain.trade

import scalaz._
import Scalaz._

object Trades {
  type Instrument = String
  type Account = String
  type NetAmount = BigDecimal

  case class Trade(account: Account, instrument: Instrument, refNo: String, market: Market, 
    unitPrice: BigDecimal, quantity: BigDecimal)

  sealed trait Market
  case object HongKong extends Market
  case object Singapore extends Market
  case object NewYork extends Market
  case object Tokyo extends Market
  case object Other extends Market

  def makeMarket(m: String) = m match {
    case "HongKong" => HongKong
    case "Singapore" => Singapore
    case "NewYork" => NewYork
    case "Tokyo" => Tokyo
    case _ => Other
  }

  sealed trait TaxFeeId
  case object TradeTax extends TaxFeeId
  case object Commission extends TaxFeeId
  case object VAT extends TaxFeeId

  val rates: Map[TaxFeeId, BigDecimal] = Map(TradeTax -> 0.2, Commission -> 0.15, VAT -> 0.1)

  val taxFeeForMarket: Map[Market, List[TaxFeeId]] = Map(Other -> List(TradeTax, Commission), Singapore -> List(TradeTax, Commission, VAT))

  def forTrade(trade: Trade) = 
    (trade, taxFeeForMarket.get(trade.market) <+> taxFeeForMarket.get(Other))

  def principal(trade: Trade) = trade.unitPrice * trade.quantity
  def valueAs(trade: Trade, tid: TaxFeeId) = ((rates get tid) map (_ * principal(trade))) getOrElse (BigDecimal(0))

  def taxFees(ts: (Trade, Option[List[TaxFeeId]])) = ts._2 match {
    case Some(l) => (ts._1, l.zip(l.map(valueAs(ts._1, _))))
    case None => (ts._1, List())
  }

  def validQuantity(qty: BigDecimal): Validation[String, BigDecimal] = 
    try {
      if (qty <= 0) "qty must be > 0".fail
      else if (qty > 500) "qty must be <= 500".fail
      else qty.success
    } catch {
      case e => e.toString.fail
    }

  def validUnitPrice(price: BigDecimal): Validation[String, BigDecimal] = 
    try {
      if (price <= 0) "price must be > 0".fail
      else if (price > 100) "price must be <= 100".fail
      else price.success
    } catch {
      case e => e.toString.fail
    }

  def makeTrade(m: Map[String, String]): Option[Trade] =
    m.get("account") |@| 
      m.get("instrument") |@| 
        m.get("refNo") |@| 
          (m.get("market") map (makeMarket(_))) |@| 
            (m.get("unitPrice") map (BigDecimal(_))) |@| 
              (m.get("quantity") map (BigDecimal(_))) apply Trade.apply

  case class RichTrade(trade: Trade, taxFees: Map[TaxFeeId, BigDecimal])
  def enrichWith(ts: (Trade, List[(TaxFeeId, BigDecimal)])) = 
    RichTrade(ts._1, Map[TaxFeeId, BigDecimal]() ++ ts._2)
  def netAmount(rt: RichTrade) = rt.taxFees.foldLeft(principal(rt.trade))((a, b) => a + b._2)
}
