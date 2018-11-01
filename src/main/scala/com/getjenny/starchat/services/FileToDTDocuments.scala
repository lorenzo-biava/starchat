package com.getjenny.starchat.services

import java.io.{File, FileReader}

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import breeze.io.CSVReader
import com.getjenny.starchat.entities.DTDocument
import com.getjenny.starchat.serializers.JsonSupport
import scalaz.Scalaz._

import scala.collection.immutable.{List, Map}
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor, Future}

object FileToDTDocuments extends JsonSupport {

  def getDTDocumentsFromCSV(log: LoggingAdapter, file: File, skipLines: Int = 1, separator: Char = ','):
  List[DTDocument] = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    val refnumcol = 11
    val fileReader = new FileReader(file)
    lazy val fileEntries = CSVReader.read(input=fileReader, separator=separator,
      quote = '"', skipLines=skipLines)

    val dtDocuments: List[DTDocument] = fileEntries.map(entry => {
      if (entry.length =/= refnumcol) {
        val message = "file row is not consistent  Row(" + entry.toString + ")"
        throw new Exception(message)
      } else {

        val queriesCsvString = entry(4)
        val actionInputCsvString = entry(7)
        val stateDataCsvString = entry(8)

        val queriesFuture: Future[List[String]] = queriesCsvString match {
          case "" => Future { List.empty[String] }
          case _ => Unmarshal(queriesCsvString).to[List[String]]
        }

        val actionInputFuture: Future[Map[String, String]] = actionInputCsvString match {
          case "" => Future { Map.empty[String, String] }
          case _ => Unmarshal(actionInputCsvString).to[Map[String, String]]
        }

        val stateDataFuture: Future[Map[String, String]] = stateDataCsvString match {
          case "" => Future { Map.empty[String, String] }
          case _ => Unmarshal(stateDataCsvString).to[Map[String, String]]
        }

        val queries = Await.result(queriesFuture, 10.second)
        val actionInput = Await.result(actionInputFuture, 10.second)
        val stateData = Await.result(stateDataFuture, 10.second)

        val document = DTDocument(state = entry(0),
          executionOrder = entry(1).toInt,
          maxStateCount = entry(2).toInt,
          analyzer = entry(3),
          queries = queries,
          bubble = entry(5),
          action = entry(6),
          actionInput = actionInput,
          stateData = stateData,
          successValue = entry(9),
          failureValue = entry(10)
        )

        document
      }
    }).toList
    dtDocuments
  }
}
