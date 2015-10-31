package com.openquant.quoter.historical

import java.util.Date

import com.openquant.quoter.common.{Interval, Bar, Contract}
import org.joda.time.DateTime

import scala.collection.mutable
import scala.util.Try

/**
 * @author piotr 24.05.15
 */
trait QuoteProvider {
  //def available(symbol: Symbol, interval: Interval.Interval = Interval._1_day): Boolean

  def quotes(contract: Contract, startDate: Date, endDate: Date = new DateTime().minusMinutes(5).toDate,
    interval: Interval.Interval = Interval._1_day): Try[mutable.Map[Long, Bar]]

  def available(contract: Contract, interval: Interval.Interval): Boolean
}

object QuoteProvider {
  def apply(source: Source.Value = Source.YahooFinance): QuoteProvider = source match {
    case Source.YahooFinance ⇒ YahooQuoteProvider()
  }
}

