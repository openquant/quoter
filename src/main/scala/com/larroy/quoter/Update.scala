package com.openquant.quoter

import java.util.Date

import com.openquant.quoter.common.Interval.Interval
import com.openquant.quoter.common._
import com.openquant.quoter.historical.{QuoteProvider, Source}
import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import quotedb._
import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Failure, Try, Success}

/**
 * @author piotr 18.05.15
 */
class Update(quoteProvider: QuoteProvider, quoteDB: QuoteDB) {
  private val log: Logger = LoggerFactory.getLogger(this.getClass)

  def updateSymbol(contract: Contract, start: Date, end: Date, interval: Interval): Try[Unit] = {
    quoteProvider.quotes(contract, start, end, interval) map { dateBar ⇒
      if (dateBar.nonEmpty) {
        log.debug(s"updateSymbol ${contract.symbol} ${dateBar.size} quotes [${new Date(dateBar.keys.head).toString}, ${new Date(dateBar.keys.last).toString}]")
        quoteDB.upsert(contract, dateBar.values, interval)
      } else
        quoteDB.writeLastUpdateAttempt(contract, interval, UpdateStatus.Success)
    } match {
      case x @ Failure(e) ⇒
        quoteDB.writeLastUpdateAttempt(contract, interval, UpdateStatus.Failure)
        log.error(s"quotes for Symbol: ${contract} failed")
        x
      case x @ Success(_) ⇒ x
    }
  }

  def update(contract: Contract, interval: Interval): Try[Unit] = {
    quoteDB.lastDate(contract, interval).foreach { last ⇒
      return updateSymbol(contract, last, Update.defaultEndDate, interval)
    }
    Failure(new RuntimeException("subscription not found"))
  }

  def updateAll(): Unit = {
    quoteDB.subscriptions().foreach { subs ⇒
      val start: Date = subs.lastUpdateStatus.filter{ _ == UpdateStatus.Success }.flatMap { x ⇒ subs.lastUpdateAttempt }.getOrElse(Update.defaultStartDate.get)
      val end: Date = Update.defaultEndDate
      val df = ISODateTimeFormat.dateHourMinuteSecond()
      import utils.dateToDateTime
      log.info(s"updateSymbol ${subs.symbol} [${df.print(start)}, ${df.print(end)}] ${subs.interval}")
      updateSymbol(subs.symbol, start, end, subs.interval)
    }
  }
}

object Update {
  val cfg = ConfigFactory.load().getConfig("quoter")
  def defaultStartDate = utils.dateFromISO(cfg.as[String]("defaultStartDate"))
  def defaultEndDate = new DateTime(DateTimeZone.UTC).minusMinutes(1).toDate

  def apply(options: Options): Unit = {
    val quoteDB = QuoteDB(options.quoteDBUrl)
    val update = new Update(QuoteProvider(options.source), quoteDB)
    if (options.symbol.isEmpty)
      update.updateAll()
    else {
      val sym = Contract(options.symbol, options.contractType, options.exchange, options.currency, options.maybeExpiry)
      val startDate = List(options.maybeStartDate, quoteDB.lastDate(sym, options.interval), defaultStartDate).flatten.headOption.get
      val endDate = options.maybeEndDate.getOrElse(defaultEndDate)

      update.updateSymbol(
        sym,
        startDate,
        endDate,
        options.interval
      )
    }
  }
}
