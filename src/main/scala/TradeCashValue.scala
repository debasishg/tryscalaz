package net.debasishg.domain.trade.dsl

trait CashValue {

  import TradeModel._
  type NetAmount = BigDecimal
  type CashValueCalculationStrategy = PartialFunction[Market, Trade => NetAmount]

  val cashValue: Trade => CashValueCalculationStrategy => NetAmount = { trade => pf =>
    if (pf.isDefinedAt(trade.market)) pf(trade.market)(trade)
    else cashValueComputation(trade)
  }

  val cashValueComputation: Trade => NetAmount = { trade => 
    (forHongKong orElse forSingapore orElse forDefault)(trade.market)(trade)
  }

  lazy val cashVal = { pf: CashValueCalculationStrategy =>
    pf orElse cashValueComp
  }

  lazy val cashValueComp: CashValueCalculationStrategy = 
    forHongKong orElse forSingapore orElse forDefault

  val forHongKong: CashValueCalculationStrategy = {
    case HongKong => { trade => BigDecimal(100) }
  }

  val forSingapore: CashValueCalculationStrategy = {
    case Singapore => { trade => BigDecimal(200) }
  }

  val forDefault: CashValueCalculationStrategy = {
    case _ => { trade => BigDecimal(300) }
  }
}

object CashValue extends CashValue
