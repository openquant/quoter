package com.openquant.quoter

import java.util.Date

import com.openquant.quoter.common.Interval
import com.openquant.quoter.common.Interval.Interval
import org.joda.time.{Hours, Minutes, Seconds, Days}
import com.openquant.quoter.utils.dateToDateTime

/**
 * Created by piotr on 6/13/15.
 */
package object historical {
  def intervalsBetween(start: Date, end: Date, interval: Interval): Long = {
    val days = Math.abs(Days.daysBetween(start, end).getDays)
    interval match {
      case Interval._1_secs =>
        Math.abs(Seconds.secondsBetween(start, end).getSeconds)

      case Interval._5_secs =>
        Math.abs(Seconds.secondsBetween(start, end).getSeconds) / 5

      case Interval._10_secs =>
        Math.abs(Seconds.secondsBetween(start, end).getSeconds) / 10

      case Interval._15_secs =>
        Math.abs(Seconds.secondsBetween(start, end).getSeconds) / 15

      case Interval._30_secs =>
        Math.abs(Seconds.secondsBetween(start, end).getSeconds) / 30

      case Interval._1_min =>
        Math.abs(Minutes.minutesBetween(start, end).getMinutes)

      case Interval._2_mins =>
        Math.abs(Minutes.minutesBetween(start, end).getMinutes) / 2

      case Interval._3_mins =>
        Math.abs(Minutes.minutesBetween(start, end).getMinutes) / 3

      case Interval._5_mins =>
        Math.abs(Minutes.minutesBetween(start, end).getMinutes) / 5

      case Interval._10_mins =>
        Math.abs(Minutes.minutesBetween(start, end).getMinutes) / 10

      case Interval._15_mins =>
        Math.abs(Minutes.minutesBetween(start, end).getMinutes) / 15

      case Interval._20_mins =>
        Math.abs(Minutes.minutesBetween(start, end).getMinutes) / 20

      case Interval._30_mins =>
        Math.abs(Minutes.minutesBetween(start, end).getMinutes) / 30

      case Interval._1_hour =>
        Math.abs(Hours.hoursBetween(start, end).getHours)

      case Interval._4_hours =>
        Math.abs(Hours.hoursBetween(start, end).getHours) / 4

      case Interval._1_day =>
        Math.abs(Days.daysBetween(start, end).getDays)

      case Interval._1_week =>
        Math.abs(Days.daysBetween(start, end).getDays) / 7
    }
  }
}
