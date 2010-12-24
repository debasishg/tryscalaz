package net.debasishg.domain.trade.dsl

/**
 * Created by IntelliJ IDEA.
 * User: debasish
 * Date: 24/12/10
 * Time: 11:02 PM
 * To change this template use File | Settings | File Templates.
 */
import scalaz._
import Scalaz._

object TradeDsl {

  import TradeModel._

  // enrichment of trade
  // implementation follows problem domain model
  // Reader monad
  val enrich = for {
    taxFeeIds      <- forTrade // get the tax/fee ids for a trade
    taxFeeValues   <- taxFeeCalculate // calculate tax fee values
    netAmount      <- enrichTradeWith // enrich trade with net amount
  }
  yield((taxFeeIds ∘ taxFeeValues) ∘ netAmount)

  // executes an Order in a market on behalf of a specific broker account
  // generates a sequence of Executions
  val execute: Market => Account => Order => Seq[Execution] = {market =>
    {brokerAccount =>
      {order =>
        order.items.map {item =>
          Execution(brokerAccount, item.ins, "e-123", market, item.price, item.qty)
        }
      }
    }
  }

  // allocates an execution to a List of client accounts
  // generates a List of trades
  val allocate: List[Account] => Execution => Seq[Validation[NonEmptyList[String], Trade]] = {accounts =>
    {execution =>
      val q = execution.quantity / accounts.size
      accounts.map {account =>
        makeTrade(account, execution.instrument, "t-123", execution.market, execution.unitPrice, q)
      }
    }
  }

  def tradeGeneration(m: Market, b: Account, c: List[Account]) =
    kleisli(execute(m)(b)) >=> kleisli(allocate(c))
}
