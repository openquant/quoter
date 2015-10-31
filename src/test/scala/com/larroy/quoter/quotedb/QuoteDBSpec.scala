package com.openquant.quoter.quotedb

/**
 * @author piotr 22.05.15
 */


import com.openquant.quoter.common.{Stock, Interval, Bar}
import org.specs2.mutable._
import scala.collection.mutable

class QuoteDBSpec extends Specification {
  "QuoteDBSpec" should {
    "create and remove subscriptions" in new TempDBScope {
      val symbol = new Stock("IBM")
      val interval = Interval._1_day
      quoteDB.subscribe(symbol, interval)

      var subs = quoteDB.subscriptions()
      subs must have size(1)
      subs(0) should beEqualTo(Subscription(symbol, interval))

      quoteDB.subscribe(symbol, interval)
      subs = quoteDB.subscriptions()
      subs must have size(1)
      subs(0) should beEqualTo(Subscription(symbol, interval))


      quoteDB.subscribe(new Stock("MSFT"), interval)
      subs = quoteDB.subscriptions()
      subs must have size(2)
      subs(1) should beEqualTo(Subscription(new Stock("MSFT"), interval))

      quoteDB.unsubscribe(new Stock("MSFT"), interval)
      subs = quoteDB.subscriptions()
      subs must have size(1)
      subs(0) should beEqualTo(Subscription(symbol, interval))
      //1 should beEqualTo(1)
    }
    "store and update bars" in new TempDBScope {
      val symbol = new Stock("IBM")
      val interval = Interval._1_day
      var quotes = Array(
        Bar(1432471235040L, 0, 1, 2, 3, 4, 5),
        Bar(1432471235041L, 10, 11, 12, 13, 14, 15)
      )
      quoteDB.upsert(symbol, quotes, interval)
      val intervals = quoteDB.availableIntervals(symbol)
      intervals.toTraversable must have size(1)
      intervals.head must beEqualTo(interval)

      def checkQuotes(qm: mutable.Map[Long, Bar]) = {
        quotes.indices.foreach { i ⇒
          qm(quotes(i).date) must beEqualTo(quotes(i))
        }
      }

      checkQuotes(quoteDB.quotes(symbol, None))

      quotes(0) = Bar(1432471235040L, 1, 1, 2, 3, 4, 5)
      quoteDB.upsert(symbol, quotes, interval)
      checkQuotes(quoteDB.quotes(symbol, None))
    }
  }
}

