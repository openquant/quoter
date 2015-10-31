package com.openquant.quoter.quotedb

import java.util.Date

import com.openquant.quoter.common.{Contract, Contract$, Interval}
import Interval.Interval
import com.openquant.quoter.historical.Source.Source
import com.openquant.quoter.historical.Source
import com.openquant.quoter.quotedb.UpdateStatus.UpdateStatus
import yahoofinance.YahooFinance

/**
 * @author piotr 22.05.15
 */
case class Subscription(symbol: Contract, interval: Interval, lastUpdateSuccess: Option[Date] = None, lastUpdateAttempt: Option[Date] = None, lastUpdateStatus: Option[UpdateStatus] = None, source: String = "")
