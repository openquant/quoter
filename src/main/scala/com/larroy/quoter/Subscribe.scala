package com.openquant.quoter

import java.util.Date

import com.openquant.quoter.common.Interval.Interval
import com.openquant.quoter.historical.{Source, QuoteProvider}
import com.openquant.quoter.quotedb.QuoteDB
import com.openquant.quoter.common._
import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Success, Try}

/**
 * @author piotr 24.05.15
 */
object Subscribe {
  private val log: Logger = LoggerFactory.getLogger(this.getClass)
  def apply(options: Options): Boolean = {
    val quoteDB = QuoteDB(options.quoteDBUrl)
    val quoteProvider = QuoteProvider(options.source)
    val sym = Contract(options.symbol, options.contractType, options.exchange, options.currency, options.maybeExpiry)
    quoteProvider.available(sym, options.interval) match {
      case true ⇒
        quoteDB.subscribe(sym, options.interval, options.source.toString)
        true
      case false =>
        false
    }
  }

  def apply(quoteDBUrl: String, symbol: String, contractType: Contract.Type.Value, interval: Interval, source: Source.Value, exchange: String, currency: String, maybeExpiry: Option[Date]): Boolean = {
    val quoteDB = QuoteDB(quoteDBUrl)
    val quoteProvider = QuoteProvider(source)
    val sym = Contract(symbol, contractType, exchange, currency, maybeExpiry)
    quoteDB.subscribe(sym, interval, source.toString)
    log.debug(s"Subscribe to ${contractType} ${symbol} ${exchange} ${interval}")
    true
    /*
    quoteProvider.available(sym, interval) match {
      case true ⇒
        quoteDB.subscribe(sym, interval, source)
        true
      case false =>
        false
    }
    */
  }

  def all(): Unit = {
    var stockList = StockList.listExchange(StockList.ExchangeType.NYSE)
    stockList.foreach { stockInfo ⇒
      apply("", stockInfo.ticker, Contract.Type.Stock, Interval._1_day, Source.YahooFinance, "NYSE", "USD", None)
    }
  }
}
