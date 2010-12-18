package net.debasishg.domain.trade.dsl

import scalaz._
import Scalaz._

object Trades {
  type Instrument = String
  type Account = String
  type NetAmount = BigDecimal

  // the main domain class
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
    try {
      if (qty <= 0) "qty must be > 0".fail
      else if (qty > 500) "qty must be <= 500".fail
      else qty.success
    } catch {
      case e => e.toString.fail
    }

  // validate unit price
  def validUnitPrice(price: BigDecimal): Validation[String, BigDecimal] = 
    try {
      if (price <= 0) "price must be > 0".fail
      else if (price > 100) "price must be <= 100".fail
      else price.success
    } catch {
      case e => e.toString.fail
    }

  def makeTrade(account: Account, instrument: Instrument, refNo: String, market: Market, 
    unitPrice: BigDecimal, quantity: BigDecimal) =
    (validUnitPrice(unitPrice).liftFailNel |@| 
      validQuantity(quantity).liftFailNel) { (u, q) => Trade(account, instrument, refNo, market, u, q) }

  def makeTrade(m: Map[String, String]): Option[Trade] =
    m.get("account") |@| 
      m.get("instrument") |@| 
        m.get("refNo") |@| 
          (m.get("market") map (makeMarket(_))) |@| 
            (m.get("unitPrice") map (BigDecimal(_))) |@| 
              (m.get("quantity") map (BigDecimal(_))) apply Trade.apply

  val enrichTradeWith: Trade => List[(TaxFeeId, BigDecimal)] => BigDecimal = {trade => {taxes => 
    taxes.foldLeft(principal(trade))((a, b) => a + b._2)
  }}

  // Reader monad
  val lifecycle = for {
    taxFeeIds <- forTrade // get the tax/fee ids for a trade
    taxFeeValues <- taxFeeCalculate // calculate tax fee values
    netAmount <- enrichTradeWith // enrich trade with net amount
  }
  yield((taxFeeIds ∘ taxFeeValues) ∘ netAmount)
}
