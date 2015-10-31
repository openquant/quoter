package com.openquant.quoter.quotedb

import java.util.Date

import com.openquant.quoter.common
import com.openquant.quoter.common.Interval.Interval
import com.openquant.quoter.common.{Bar, Interval}
import com.typesafe.config.ConfigFactory

import scala.collection.mutable


object UpdateStatus extends Enumeration {
  type UpdateStatus = Value
  val Success, Failure = Value
}

/**
 * @author piotr 03.05.15
 */
trait QuoteDB {

  def initialize(): Unit

  /**
   * Activate subscription for a contract at a given interval
   * @param interval granularity of the data
   * @param source name of the data source
   * @return true if a new subscription is created or false if it can't be created (for example if it already exists)
   */
  def subscribe(contract: common.Contract, interval: Interval, source: String = ""): Boolean
  def unsubscribe(contract: common.Contract, interval: Interval): Boolean
  def subscriptions(): Seq[Subscription]

  /**
   * Get the [[Subscription]] information about a contract if exists
   * @param contract
   * @param interval
   * @return
   */
  def subscription(contract: common.Contract, interval: Interval): Option[Subscription]

  /**
   * Last date the contract was updated
   * @param contract
   * @return an optional Date if the contract exists
   */
  def lastDate(contract: common.Contract, interval: Interval = Interval._1_day): Option[Date]

  /**
   * Date of first record of the contract
   * @param contract
   * @param interval
   * @return
   */
  def firstDate(contract: common.Contract, interval: Interval = Interval._1_day): Option[Date]

  /**
   * Get quotes for a contract, optionally in the range given by startDate and endDate
   * @param contract
   * @param startDate
   * @param endDate
   * @return
   */
  def quotes(contract: common.Contract, startDate: Option[Date], endDate: Date = new Date(), interval: Interval = Interval._1_day): mutable.Map[Long, Bar]

  /**
   * Update or insert the given quotes for a contract
   */
  def upsert(contract: common.Contract, quotes: Iterable[Bar], interval: Interval = Interval._1_day): Unit

  /**
   * Write in the database the given date as the last time when an update was attempted
   */
  def writeLastUpdateAttempt(contract: common.Contract, interval: Interval, updateStatus: UpdateStatus.Value, date: Date = new Date()): Unit

  /**
   * @return Number of quotes for a contract
   */
  def count(contract: common.Contract, interval: Interval = Interval._1_day): Long

  /**
   * @return set of intervals which we have quotes for
   */
  def availableIntervals(contract: common.Contract): Set[Interval]

}

object QuoteDB {
  import net.ceedubs.ficus.Ficus._
  private val sqlite = "sqlite://(.*?)".r
  private val cfg = ConfigFactory.load().getConfig("quoter")

  def apply(url: String): QuoteDB = url match {
    case "" ⇒ apply()
    case sqlite(path) ⇒ new QuoteDBSQLite(path)
    case _ ⇒ throw new RuntimeException(s"No implementation of QuoteDB with the provided url '$url' found")
  }

  def apply(): QuoteDB = apply(cfg.as[String]("quoteDBUrl"))
}

