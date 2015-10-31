package com.openquant.quoter

import java.util.Date

import com.openquant.quoter.common.{Interval, Contract}
import com.openquant.quoter.historical.Source
import org.joda.time.format.DateTimeFormat
import org.slf4j.bridge.SLF4JBridgeHandler
import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Failure, Success, Try}

object Mode extends Enumeration {
  type Mode = Value
  val Invalid, Update, Subscribe, SubscribeAll = Value
}
import com.openquant.quoter.Mode._

sealed case class Options(
  mode: Mode = Mode.Invalid,
  quiet: Boolean = false,
  symbol: String = "",
  contractType: Contract.Type.Value = Contract.Type.Stock,
  exchange: String = "NYSE",
  currency: String = "USD",
  maybeExpiry: Option[Date] = None,
  maybeStartDate: Option[Date] = None,
  maybeEndDate: Option[Date] = None,
  interval: Interval.Value = Interval._1_day,
  quoteDBUrl: String = "",
  source: Source.Value = Source.YahooFinance
  )

/**
 * @author piotr 18.05.15
 */
object Main {
  private val log: Logger = LoggerFactory.getLogger(this.getClass)
  private val version = "0.1"
  SLF4JBridgeHandler.removeHandlersForRootLogger()
  SLF4JBridgeHandler.install()



  val dateTimeFormat = DateTimeFormat.forPattern("yyyyMMdd HH:mm:ss")
  val validDateRe = """(\d{8}) (\d{2}:\d{2}:\d{2}) ?(\w*)?""".r

  def getOptionParser: scopt.OptionParser[Options] = {
    new scopt.OptionParser[Options]("vadis") {
      head("quoter", Main.version)

      override def showUsageOnError: Boolean = true

      help("help") text ("print help")
      /*
       * Common arguments
       */
      opt[Boolean]('q', "quiet") text ("suppress progress on stdout") action {
        (arg, o) => o.copy(quiet = arg)
      }

      cmd("update") text ("update") text ("Example: update -s MSFT") action {
        (_, o) => o.copy(mode = Mode.Update)
      } children(
        opt[String]('s', "symbol") text ("symbol") action {
          (arg, o) => o.copy(symbol = arg)
        },
        opt[String]('t', "type") text ("type") action {
          (arg, o) => o.copy(contractType = Contract.Type.withName(arg))
        },
        note(s"type is one of: '${Contract.Type.values.mkString(", ")}'"),
        opt[String]('e', "exchange") text ("exchange") action {
          (arg, o) => o.copy(exchange = arg)
        },
        opt[String]('c', "currency") text ("currency") action {
          (arg, o) => o.copy(currency = arg)
        },

        opt[String]('y', "expiry") text ("expiry") action {
          (arg, o) => o.copy(maybeExpiry = Some(dateTimeFormat.parseDateTime(arg).toDate))
        } validate {
          case validDateRe(_*) ⇒ success
          case _ ⇒ failure(s"argument doesn't match ${validDateRe.toString}")
        },

        opt[String]('r', "source") text ("source") action {
          (arg, o) => o.copy(source = Source.withName(arg))
        },

        opt[String]('i', "interval") text ("interval") action {
          (arg, o) => o.copy(interval = Interval.withName(arg))
        },
        note(s"interval is one of: '${Interval.values.mkString(", ")}'"),
        opt[String]('d', "dburl") text ("dburl") action {
          (arg, o) => o.copy(quoteDBUrl = arg)
        }
      )

      cmd("subscribeall") action {
        (_, o) => o.copy(mode = Mode.SubscribeAll)
      }

      cmd("subscribe") action {
        (_, o) => o.copy(mode = Mode.Subscribe)
      } children (
        opt[String]('s', "symbol") text ("symbol") minOccurs (1) action {
          (arg, o) => o.copy(symbol = arg)
        },
        opt[String]('t', "type") text ("type") action {
          (arg, o) => o.copy(contractType = Contract.Type.withName(arg))
        },
        opt[String]('e', "exchange") text ("exchange") action {
          (arg, o) => o.copy(exchange = arg)
        },
        opt[String]('c', "currency") text ("currency") action {
          (arg, o) => o.copy(currency = arg)
        },

        opt[String]('y', "expiry") text ("expiry") action {
          (arg, o) => o.copy(maybeExpiry = Some(dateTimeFormat.parseDateTime(arg).toDate))
        } validate {
          case validDateRe(_*) ⇒ success
          case _ ⇒ failure(s"argument doesn't match ${validDateRe.toString}")
        },

        opt[String]('r', "source") text ("source") action {
          (arg, o) => o.copy(source = Source.withName(arg))
        },

        opt[String]('i', "interval") text ("interval") action {
          (arg, o) => o.copy(interval = Interval.withName(arg))
        },
        note(s"interval is one of: '${Interval.values.mkString(", ")}'"),
        opt[String]('d', "dburl") text ("dburl") action {
          (arg, o) => o.copy(quoteDBUrl = arg)
        },
        opt[String]('a', "startdate") text ("startdate") action {
          (arg, o) => o.copy(maybeStartDate = Some(dateTimeFormat.parseDateTime(arg).toDate))
        } validate {
          case validDateRe(_*) ⇒ success
          case _ ⇒ failure(s"argument doesn't match ${validDateRe.toString}")
        },
        opt[String]('z', "enddate") text ("enddate") action {
          (arg, o) => o.copy(maybeEndDate = Some(dateTimeFormat.parseDateTime(arg).toDate))
        } validate {
          case validDateRe(_*) ⇒ success
          case _ ⇒ failure(s"argument doesn't match ${validDateRe.toString}")
        }
      )
    }
  }

  def main(args: Array[String]) {
    val optionParser = getOptionParser
    val options: Options = optionParser.parse(args, Options()).getOrElse {
      log.error("Option syntax incorrect")
      log.error(s"Arguments given ${args.mkString("'", "' '", "'")}")
      log.error("Failure.")
      sys.exit(1)
    }
    def exitSuccess(): Unit = {
      log.info("Success.")
      log.info("=========== finished successfully ================")
      sys.exit(0)
    }

    def exitFailure(): Unit = {
      log.error("Failure.")
      log.info("=========== finished with errors =================")
      sys.exit(-1)
    }

    Try(options.mode match {
      case Mode.Invalid ⇒
        optionParser.reportError("Please specify a valid command")
        optionParser.showUsage
        exitFailure()

      case Mode.Update ⇒
        Update(options)

      case Mode.SubscribeAll ⇒
        Subscribe.all()

      case Mode.Subscribe ⇒
        if(Subscribe(options))
          exitSuccess()
        else {
          log.error(s"Subscription to ${options.symbol} failed (Options: ${options})")
          exitFailure()
        }

      case _ =>
        log.error("Unknown mode")
        exitFailure()

    }) match {
      case Success(x) ⇒
        exitSuccess()

      case Failure(e) ⇒
        log.error(s"Exception thrown ${e.getMessage}")
        e.printStackTrace()
        exitFailure()
    }
  }
}
