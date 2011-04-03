package net.debasishg.domain.trade.dsl

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class CashValueSpec extends Spec with ShouldMatchers {
  describe("cash value calculation") {
    import TradeModel._
    import CashValue._

    val tHkg = Trade("a-123", "google", "r-123", HongKong, 12.25, 200)
    val tTky = Trade("a-125", "ibm", "r-125", Tokyo, 22.25, 250)
    val tSgp = Trade("a-126", "cisco", "r-126", Singapore, 25, 300) 
    val tNyse = Trade("a-128", "cisco", "r-126", NewYork, 25, 300) 

    it("should use appropriate algorithm") {
      cashValue(tSgp) {
        case NewYork => { trade => BigDecimal(124) }
      } should equal(200)

      cashValue(tTky) {
        case NewYork => { trade => BigDecimal(124) }
      } should equal(300)

      cashValue(tTky) {
        case Tokyo => { trade => BigDecimal(124) }
      } should equal(124)

      val pf: PartialFunction[Market, Trade => CashValue.NetAmount] = {
        case Tokyo => { trade => BigDecimal(125) }
      }

      (pf orElse cashValueComp)(tTky.market)(tTky) should equal(125)
      (pf orElse cashValueComp)(tNyse.market)(tNyse) should equal(300)
      cashValueComp(tNyse.market)(tNyse) should equal(300)
      cashVal(pf)(tNyse.market)(tNyse) should equal(300)
    }
  }
}
