package com.openquant.quoter.historical

import java.util.{TimeZone, Calendar, Date}

import com.openquant.quoter.common.Interval.Interval
import com.openquant.quoter.common.{Contract, Interval, Bar, Contract$}
import com.openquant.quoter.utils.dateToDateTime
import org.joda.time.{Days, DateTime, DateTimeZone}
import yahoofinance.histquotes.HistoricalQuote
import scala.collection.mutable
import scala.collection.JavaConversions._
import scala.util.{Success, Try}

import org.slf4j.{Logger, LoggerFactory}


/**
 * @author piotr 24.05.15
 */
class YahooQuoteProvider extends QuoteProvider {
  private val log: Logger = LoggerFactory.getLogger(this.getClass)
  private def toYahooInterval(x: Interval): yahoofinance.histquotes.Interval = x match {
    case Interval._1_day ⇒ yahoofinance.histquotes.Interval.DAILY
    case Interval._1_week ⇒ yahoofinance.histquotes.Interval.WEEKLY
  }

  /**
   * Reify quotes as a map of epoch time to [[Bar]]
   * @param quotes
   * @return
   */
  private def reifyQuotes(quotes: Seq[HistoricalQuote]): mutable.Map[Long, Bar] = {
    // Change the type of quotes to our convention of Map of time to Bar
    val dateBar = quotes.map { q ⇒
      val dateEpoch = q.getDate.getTime.getTime
      dateEpoch → Bar(dateEpoch, q.getOpen.doubleValue, q.getClose.doubleValue, q.getHigh.doubleValue,
        q.getLow.doubleValue, q.getAdjClose.doubleValue, q.getVolume.toDouble, Source.YahooFinance
      )
    }
    import collection.JavaConverters._
    val result = new java.util.TreeMap[Long, Bar].asScala
    dateBar.foreach(result.+=)
    result
  }

  /**
   * Try to get quotes for a contract in the timeframe defined by the range of dates and the time interval
   * @param contract
   * @param startDate
   * @param endDate
   * @param interval
   * @return
   */
  override def quotes(contract: Contract, startDate: Date, endDate: Date, interval: Interval): Try[mutable.Map[Long, Bar]] = {
    val start = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    start.setTime(startDate)

    val end = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    end.setTime(endDate)

    // avoid any IO when there's no data
    val numIntervals = intervalsBetween(startDate, endDate, interval)
    if (numIntervals == 0) {
      log.debug(s"YahooQuoteProvider, interval [${startDate}, ${endDate}] has 0 intervals")
      Success(mutable.Map.empty[Long, Bar])
    } else {
      val stock = new yahoofinance.Stock(contract.symbol)
      Try(stock.getHistory(start, end, toYahooInterval(interval))).map { quotes ⇒ reifyQuotes(quotes) }
    }
  }

  override def available(contract: Contract, interval: Interval.Interval): Boolean = {
    val stock = new yahoofinance.Stock(contract.symbol)

    val start = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    val startD = new DateTime(DateTimeZone.UTC).minusDays(2).toDate
    start.setTime(startD)
    val endD = new DateTime(DateTimeZone.UTC).minusDays(1).toDate
    val end = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    end.setTime(endD)

    stock.getHistory(start, end, toYahooInterval(interval)).size match {
      case 0 ⇒ false
      case _ ⇒ true
    }
  }
}

object YahooQuoteProvider {
  def apply(): YahooQuoteProvider = new YahooQuoteProvider
}
