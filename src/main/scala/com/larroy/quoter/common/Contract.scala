package com.openquant.quoter.common

import java.util.Date

import com.openquant.quoter.utils


/**
 * @author piotr 05.05.15
 */
sealed trait Contract {
  val symbol: String
  val exchange: String
  val currency: String
  val expiry: Option[Date] = None
  val expiryIso: String = expiry.map(utils.dateToISO).getOrElse("")
  def contractType: String = this match {
    case _: Stock ⇒ Contract.Type.Stock.toString
    case _: Future ⇒ Contract.Type.Future.toString
    case _: Currency ⇒ Contract.Type.Currency.toString
  }
}

case class Future(symbol: String, exchange: String, currency: String = "USD", override val expiry: Option[Date]) extends Contract

case class Stock(symbol: String, exchange: String = "NYSE", currency: String = "USD") extends Contract

case class Currency(symbol: String, exchange: String = "IDEALPRO", currency: String = "USD") extends Contract


object Contract {
  object Type extends Enumeration {
    type SymbolType = Value
    val Future, Stock, Currency = Value
  }
  def apply(symbol: String, symbolType: Type.SymbolType, exchange: String, currency: String, expiry: Option[Date]): Contract = {
    symbolType match {
      case Type.Future ⇒ Future(symbol, exchange, currency, expiry)
      case Type.Stock ⇒ Stock(symbol, exchange, currency)
      case Type.Currency ⇒ Currency(symbol, exchange, currency)
    }
  }
  def apply(symbol: String, symbolType: String, exchange: String, currency: String = "USD", expiry: Option[Date] = None): Contract =
    Contract.apply(symbol, Type.withName(symbolType), exchange, currency, expiry)
}
