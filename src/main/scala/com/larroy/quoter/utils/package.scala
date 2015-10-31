package com.openquant.quoter

import java.util.Date
import org.slf4j.{Logger, LoggerFactory}

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

import scala.util.Try

/**
 * @author piotr 25.05.15
 */
package object utils {
  def dateFromISO(x: String): Option[Date] = {
    val df = ISODateTimeFormat.dateTimeNoMillis()
    Try(df.parseDateTime(x).toDate).toOption
  }

  def dateToISO(x: Date): String = {
    val df = ISODateTimeFormat.dateTimeNoMillis()
    df.print(new DateTime(x))
  }

  def time[R](block: => R): R = {
    val log: Logger = LoggerFactory.getLogger("timer")
    val t0 = System.currentTimeMillis()
    val result = block // call-by-name
    val t1 = System.currentTimeMillis()
    log.info("Elapsed time: " + (t1 - t0) + " ms")
    result
  }

  import scala.language.implicitConversions
  implicit def dateToDateTime(x: Date) = new DateTime(x)
}
