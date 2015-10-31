package com.openquant.quoter.quotedb

import java.util.Date

import com.openquant.quoter.historical.Source
import com.openquant.quoter.historical.Source.Source
import com.openquant.quoter.{utils, common}
import com.openquant.quoter.common.{Bar, Interval}
import Interval.Interval
import org.slf4j.{Logger, LoggerFactory}
import scala.collection.mutable

import collection.JavaConverters._

import scalikejdbc._

import scala.util._

/*
object QuoteDBSQLite {
  implicit def interval(x: Int): Interval = {
    Interval.apply(x)
  }

  implicit def interval(x: Interval): Int = {
    x.id
  }
}
*/

/**
 * @author piotr 15.05.15
 */
class QuoteDBSQLite(dbPath: String) extends QuoteDB {
  Class.forName("org.sqlite.JDBC")
  private val log: Logger = LoggerFactory.getLogger(this.getClass)

  ConnectionPool.add(dbPath, s"jdbc:sqlite:$dbPath", "", "")

  initialize()

  def commit[A](name: scala.Any)(execution: scala.Function1[scalikejdbc.DBSession, A]): A = {
    using(ConnectionPool.borrow(name)) { conn => DB(conn) autoCommit execution }
  }

  def readOnly[A](name: scala.Any)(execution: scala.Function1[scalikejdbc.DBSession, A]): A = {
    using(ConnectionPool.borrow(name)) { conn => DB(conn).autoClose(true).readOnly(execution) }
  }


  private def createQuotesTable(): Unit = {
     /*
     Interval, int mapped to:

     _1_secs, _5_secs, _10_secs, _15_secs, _30_secs, _1_min, _2_mins, _3_mins, _5_mins, _10_mins, _15_mins, _20_mins, _30_mins, _1_hour, _4_hours, _1_day, _1_week;

     */
    commit(dbPath) { session ⇒
      session.update(
        s"""
           |CREATE TABLE IF NOT EXISTS quotes  (
           | symbol string not null,
           | contractType string not null,
           | exchange string not null,
           | currency string not null,
           | expiry string not null default "", /* date in ISO format http://joda-time.sourceforge.net/apidocs/org/joda/time/format/ISODateTimeFormat.html */
           | interval int not null,
           | date bigint not null,
           | open real,
           | close real,
           | high real,
           | low real,
           | adjclose real,
           | volume real,
           | source string,
           | PRIMARY KEY (symbol, contractType, exchange, currency, expiry, interval, date)
           |)
         """.stripMargin
      )
    }
  }

  private def createSubscriptionsTable(): Unit = {
    commit(dbPath) { session ⇒
      session.update(
        s"""
           |CREATE TABLE IF NOT EXISTS subscriptions (
           | symbol string not null,
           | contractType string not null,
           | exchange string not null,
           | currency string not null,
           | expiry string not null default "",
           | interval int not null,
           | source string not null default "",
           | lastUpdateSuccess string not null default "",
           | lastUpdateAttempt string not null default "",
           | lastUpdateStatus string not null default "",
           | PRIMARY KEY (symbol, contractType, exchange, currency, expiry, interval)
           |)
         """.stripMargin
      )
    }
  }

  override def subscribe(contract: common.Contract, interval: Interval, source: String): Boolean = {
    Try(commit(dbPath) { session ⇒
      session.update("INSERT INTO subscriptions (symbol, contractType, exchange, currency, expiry, interval, source) VALUES (?,?,?, ?,?,?, ?)",
        contract.symbol,
        contract.contractType,
        contract.exchange,
        contract.currency,
        contract.expiryIso,
        interval.id,
        source
      )
    }) match {
      case Success(_) ⇒ true
      case Failure(_) ⇒ false
    }
  }

  override def unsubscribe(contract: common.Contract, interval: Interval): Boolean = {
    val count = commit(dbPath) { session ⇒
      session.update("DELETE FROM subscriptions WHERE symbol = ? AND contractType = ? AND exchange = ? AND currency = ? AND expiry = ? AND interval = ?",
        contract.symbol,
        contract.contractType,
        contract.exchange,
        contract.currency,
        contract.expiryIso,
        interval.id
      )
    }
    if (count > 0)
      true
    else
      false
  }


  override def subscriptions(): Seq[Subscription] = {
    commit(dbPath) { session ⇒
      val res = session.list(
        """
          | SELECT symbol, contractType, exchange, currency, expiry, interval, lastUpdateSuccess, lastUpdateAttempt, lastUpdateStatus, source
          | FROM subscriptions
          | ORDER BY symbol, exchange
        """.stripMargin
      ) { rs ⇒
        Subscription(
          common.Contract(rs.string(1), rs.string(2), rs.string(3), rs.string(4), utils.dateFromISO(rs.string(5))),
          Interval(rs.int(6)),
          utils.dateFromISO(rs.string(7)),
          utils.dateFromISO(rs.string(8)),
          Try(UpdateStatus.withName(rs.string(9))).toOption,
          rs.string(10)
        )
      }
      res
    }
  }

  override def subscription(contract: common.Contract, interval: Interval): Option[Subscription] = {
     commit(dbPath) { session ⇒
       val res = session.single(
         """
           | SELECT symbol, contractType, exchange, currency, expiry, interval,
           |  lastUpdateSuccess lastUpdateAttempt, lastUpdateStatus, source
           | FROM subscriptions
           | WHERE
           |   symbol = ?
           |   AND contractType = ?
           |   AND exchange = ?
           |   AND currency = ?
           |   AND expiry = ?
           |   AND interval = ?
           | ORDER BY symbol, exchange
         """.stripMargin,
         contract.symbol,
         contract.contractType,
         contract.exchange,
         contract.currency,
         contract.expiryIso,
         interval.id
       ) { rs ⇒
          Subscription(
            common.Contract(rs.string(1), rs.string(2), rs.string(3), rs.string(4), utils.dateFromISO(rs.string(5))),
            Interval(rs.int(6)),
            utils.dateFromISO(rs.string(7)),
            utils.dateFromISO(rs.string(8)),
            Try(UpdateStatus.withName(rs.string(9))).toOption,
            rs.string(10)
          )
       }
       res
    }
  }

  override def initialize(): Unit = {
    createQuotesTable()
    createSubscriptionsTable()
  }

  override def availableIntervals(contract: common.Contract): Set[Interval] = {
    commit(dbPath) { session ⇒
      val res = session.list(
        """
          | SELECT DISTINCT(interval)
          | FROM quotes
          | WHERE
          |   symbol = ?
          |   AND contractType = ?
          |   AND exchange = ?
          |   AND currency = ?
          |   AND expiry = ?
          | ORDER BY interval
        """.stripMargin,
        contract.symbol,
        contract.contractType,
        contract.exchange,
        contract.currency,
        contract.expiryIso
      ) { rs ⇒ Interval(rs.int(1)) }.toSet
      res
    }
  }

  override def count(contract: common.Contract, interval: Interval): Long = {
    commit(dbPath) { session ⇒
      val res = session.single(
        """
          | SELECT COUNT(*)
          | FROM quotes
          | WHERE
          |   symbol = ?
          |   AND contractType = ?
          |   AND exchange = ?
          |   AND currency = ?
          |   AND expiry = ?
          |   AND interval = ?
        """.stripMargin,
        contract.symbol,
        contract.contractType,
        contract.exchange,
        contract.currency,
        contract.expiryIso,
        interval.id
      ) { rs ⇒ rs.int(1) }.getOrElse(0)
      res
    }
  }

  override def firstDate(contract: common.Contract, interval: Interval): Option[Date] = {
    if (count(contract, interval) > 0) {
      commit(dbPath) { session ⇒
        val maybeEpoch = session.single(
          """
            | SELECT MIN(date)
            | FROM quotes
            | WHERE
            |   symbol = ?
            |   AND contractType = ?
            |   AND exchange = ?
            |   AND currency = ?
            |   AND expiry = ?
            |   AND interval = ?
          """.stripMargin,
          contract.symbol,
          contract.contractType,
          contract.exchange,
          contract.currency,
          contract.expiryIso,
          interval.id
        ) { rs ⇒ rs.int(1) }
        maybeEpoch.map(ms ⇒ new Date(ms))
      }
    } else
      None
  }

  /**
   * @param contract
   * @return an optional Date if the contract exists
   */
  override def lastDate(contract: common.Contract, interval: Interval): Option[Date] = {
    if (count(contract, interval) > 0) {
      commit(dbPath) { session ⇒
        val maybeEpoch = session.single(
          """
            | SELECT MAX(date)
            | FROM quotes
            | WHERE
            |   symbol = ?
            |   AND contractType = ?
            |   AND exchange = ?
            |   AND currency = ?
            |   AND expiry = ?
            |   AND interval = ?
          """.stripMargin,
          contract.symbol,
          contract.contractType,
          contract.exchange,
          contract.currency,
          contract.expiryIso,
          interval.id
        ) { rs ⇒ rs.long(1) }
        maybeEpoch.map(ms ⇒ new Date(ms))
      }

    } else
      None
  }

  /**
   * @param contract
   * @param startDate
   * @param endDate
   * @return
   */
  override def quotes(contract: common.Contract, startDate: Option[Date], endDate: Date, interval: Interval
    ): mutable.Map[Long, Bar] = {
    val bars = commit(dbPath) { session ⇒
      session.list(
        """
          | SELECT
          |   date,
          |   open,
          |   close,
          |   high,
          |   low,
          |   adjclose,
          |   volume,
          |   source
          | FROM quotes
          | WHERE
          |   symbol = ?
          |   AND contractType = ?
          |   AND exchange = ?
          |   AND currency = ?
          |   AND expiry = ?
          |   AND interval = ?
          |   AND date >= ?
          |   AND date <= ?
        """.stripMargin,
        contract.symbol,
        contract.contractType,
        contract.exchange,
        contract.currency,
        contract.expiryIso,
        interval.id,
        startDate.map(_.getTime).getOrElse(0L),
        endDate.getTime
      ) { rs ⇒
        new Bar(rs.long(1), rs.double(2), rs.double(3), rs.double(4), rs.double(5), rs.double(6), rs.double(7), Source.withName(rs.string(8)))
      }
    }
    val dateBar = new java.util.TreeMap[Long, Bar].asScala
    bars.foreach { bar ⇒ dateBar += (bar.date → bar) }
    dateBar
  }


  override def upsert(contract: common.Contract, bars: Iterable[Bar], interval: Interval): Unit = {
    val delete: Seq[Seq[Any]] = bars.map { bar ⇒
      Seq(
        contract.symbol,
        contract.contractType,
        contract.exchange,
        contract.currency,
        contract.expiryIso,
        interval.id,
        bar.date
      )
    }.toSeq
    val insert: Seq[Seq[Any]] = bars.map { bar ⇒
      Seq(
        contract.symbol,
        contract.contractType,
        contract.exchange,
        contract.currency,
        contract.expiryIso,
        interval.id,
        bar.date,
        bar.open,
        bar.close,
        bar.high,
        bar.low,
        bar.adjClose,
        bar.volume,
        bar.source
      )
    }.toSeq
    commit(dbPath) { session ⇒
      log.debug(s"QuoteDBSQLite.upsert, delete previous quotes")

      session.execute("PRAGMA synchronous = 0")
      session.execute("PRAGMA journal_mode = OFF")
      session.execute("PRAGMA cache_size = 64000")


      utils.time {
        session.batch(
          """
          | DELETE FROM quotes
          | WHERE
          |   symbol = ?
          |   AND contractType = ?
          |   AND exchange = ?
          |   AND currency = ?
          |   AND expiry = ?
          |   AND interval = ?
          |   AND date = ?
        """.stripMargin, delete: _*)
      }

      log.debug(s"QuoteDBSQLite.upsert, insert new (${insert.size}) quotes")
      utils.time {
        session.batch(
          """
          | INSERT INTO quotes
          |   (symbol,
          |    contractType,
          |    exchange,
          |    currency,
          |    expiry,
          |    interval,
          |    date,
          |    open,
          |    close,
          |    high,
          |    low,
          |    adjclose,
          |    volume,
          |    source
          |    )
          |  VALUES
          |    (?,?,?, ?,?,?, ?,?,?, ?,?,?, ?,?)
        """.stripMargin, insert: _*)
      }

      writeLastUpdateAttempt(contract, interval, UpdateStatus.Success, new Date())
    }
  }

  override def writeLastUpdateAttempt(contract: common.Contract, interval: Interval, updateStatus: UpdateStatus.Value, date: Date): Unit = {
    commit(dbPath) { session ⇒
      session.update(
        """
          | UPDATE subscriptions SET
          |   lastUpdateAttempt = ?,
          |   lastUpdateStatus = ?
          | WHERE
          |   symbol = ?
          |   AND contractType = ?
          |   AND exchange = ?
          |   AND currency = ?
          |   AND expiry = ?
          |   AND interval = ?
        """.stripMargin,
        utils.dateToISO(date),
        updateStatus.toString,
        contract.symbol,
        contract.contractType,
        contract.exchange,
        contract.currency,
        contract.expiryIso,
        interval.id
      )

      if (updateStatus == UpdateStatus.Success) {
        val res = session.update(
          """
          | UPDATE subscriptions SET
          |   lastUpdateSuccess = ?
          | WHERE
          |   symbol = ?
          |   AND contractType = ?
          |   AND exchange = ?
          |   AND currency = ?
          |   AND expiry = ?
          |   AND interval = ?
        """.stripMargin,
          utils.dateToISO(date),
          contract.symbol,
          contract.contractType,
          contract.exchange,
          contract.currency,
          contract.expiryIso,
          interval.id
        )
      }
    }
  }
}
