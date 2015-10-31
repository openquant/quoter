package com.openquant.quoter.common

import com.openquant.quoter.historical.Source
import com.openquant.quoter.historical.Source.Source

/**
 * @author piotr 04.05.15
 */
case class Bar(date: Long, open: Double, close: Double, high: Double, low: Double, adjClose: Double, volume: Double, source: Source = Source.YahooFinance)
