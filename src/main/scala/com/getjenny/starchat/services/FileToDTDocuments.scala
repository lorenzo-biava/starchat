package com.getjenny.starchat.services

import com.getjenny.starchat.entities.DTDocument
import akka.http.scaladsl.unmarshalling.Unmarshal

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

import breeze.io.CSVReader

import scala.concurrent.Await
import scala.collection.immutable.{List, Map}
import java.io.{File, FileReader}

import akka.stream.ActorMaterializer
import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import com.getjenny.starchat.serializers.JsonSupport

import scala.concurrent.ExecutionContext.Implicits.global

object FileToDTDocuments extends JsonSupport {

  def getDTDocumentsFromCSV(log: LoggingAdapter, file: File, skiplines: Int = 1, separator: Char = ','):
  List[DTDocument] = {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher


    val refnumcol = 11
    val fileReader = new FileReader(file)
    lazy val fileEntries = CSVReader.read(input=fileReader, separator=separator,
      quote = '"', skipLines=skiplines)

    val dtDocuments: List[DTDocument] = fileEntries.map(entry => {
      if (entry.length != refnumcol) {
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
          execution_order = entry(1).toInt,
          max_state_count = entry(2).toInt,
          analyzer = entry(3),
          queries = queries,
          bubble = entry(5),
          action = entry(6),
          action_input = actionInput,
          state_data = stateData,
          success_value = entry(9),
          failure_value = entry(10)
        )
        document
      }
    }).toList
    dtDocuments
  }
}
