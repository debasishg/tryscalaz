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

import java.util.{Date, Calendar}
import akka.actor.{Actor, ActorRef}
import Actor._

object TradingService {
  import TradeModel._

  // service methods

  // create a trade : wraps the model method
  def newTrade(account: Account, instrument: Instrument, refNo: String, market: Market,
    unitPrice: BigDecimal, quantity: BigDecimal, tradeDate: Date = Calendar.getInstance.getTime) =
      makeTrade(account, instrument, refNo, market, unitPrice, quantity) // @tofix : additional args

  private[dsl] def kestrel[T](trade: T, proc: T => T)(effect: => Unit) = {
    val t = proc(trade)
    effect
    t
  }

  // enrich trade
  def doEnrichTrade(trade: Trade)(implicit eventProcessor: ActorRef) = 
    kestrel(trade, enrichTrade) { 
      eventProcessor ! TradeEnriched(trade, enrichTrade)
    }

  // add value date
  def doAddValueDate(trade: Trade)(implicit eventProcessor: ActorRef) = 
    kestrel(trade, addValueDate) { 
      eventProcessor ! ValueDateAdded(trade, addValueDate)
    }

  type TradeEvent = (Trade => Trade)
  case class TradeEnriched(trade: Trade, closure: TradeEvent)
  case class ValueDateAdded(trade: Trade, closure: TradeEvent)

  case object Snapshot

  class CommandStore(qryStore: ActorRef) extends Actor {
    private var events = Map.empty[Trade, List[TradeEvent]]

    def receive = {
      case m@TradeEnriched(trade, closure) => 
        events += ((trade, events.getOrElse(trade, List.empty[TradeEvent]) :+ closure))
        qryStore forward m
      case m@ValueDateAdded(trade, closure) => 
        events += ((trade, events.getOrElse(trade, List.empty[TradeEvent]) :+ closure))
        qryStore forward m
      case Snapshot => 
        self.reply(events.keys.map {trade =>
          events(trade).foldLeft(trade)((t, e) => e(t))
        })
    }
  }

  case object QuerySnapshot

  class QueryStore extends Actor {
    private var trades = new collection.immutable.TreeSet[Trade]()(Ordering.by(_.refNo))

    def receive = {
      case TradeEnriched(trade, closure) => 
        trades += trades.find(_ == trade).map(closure(_)).getOrElse(closure(trade))
      case ValueDateAdded(trade, closure) => 
        trades += trades.find(_ == trade).map(closure(_)).getOrElse(closure(trade))
      case QuerySnapshot =>
        self.reply(trades.toList)
    }
  }
}
