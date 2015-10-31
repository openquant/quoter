package com.openquant.quoter.common

import java.io.InputStreamReader

import com.openquant.quoter.utils
import org.joda.time.format.ISODateTimeFormat


/**
 * @author piotr 31.05.15
 */
class FutureList {

  object FutureContract extends Enumeration {
    type FutureContract = Value
    val LightCrudeOil = Value
    val BrentOil = Value
  }

  import FutureContract._

  private def getLightCrudeOilContracts(): Map[String, Future] = {
    val is = getClass.getResourceAsStream("/cl.csv")
    if (is == null)
      Map.empty[String, Future]
    import com.github.tototoshi.csv._
    val rdr = CSVReader.open(new InputStreamReader(is))
    def parseCSVLine(x: List[String]): (String, Future) = {
      val df = ISODateTimeFormat.dateHourMinuteSecond()
      val d = df.parseDateTime(x(3)).toDate()
      (x(0) → new Future("CL", "NYMEX", "USD", Some(d)))
    }
    rdr.toStream.drop(1).map(parseCSVLine).toMap
  }

  def getContracts(futureContract: FutureContract.Value): Map[String, Future] = futureContract match {
    case LightCrudeOil ⇒ getLightCrudeOilContracts()
    case _ ⇒ throw new RuntimeException(s"Contract ${futureContract} not supported")
  }
}
