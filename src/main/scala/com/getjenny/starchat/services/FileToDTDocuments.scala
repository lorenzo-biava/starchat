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
    val file_reader = new FileReader(file)
    lazy val file_entries = CSVReader.read(input=file_reader, separator=separator,
      quote = '"', skipLines=skiplines)

    val dtdocuments: List[DTDocument] = file_entries.map(entry => {
      if (entry.length != refnumcol) {
        val message = "file row is not consistent  Row(" + entry.toString + ")"
        throw new Exception(message)
      } else {

        val queries_csv_string = entry(4)
        val action_input_csv_string = entry(7)
        val state_data_csv_string = entry(8)

        val queries_future: Future[List[String]] = queries_csv_string match {
          case "" => Future { List.empty[String] }
          case _ => Unmarshal(queries_csv_string).to[List[String]]
        }

        val action_input_future: Future[Map[String, String]] = action_input_csv_string match {
          case "" => Future { Map.empty[String, String] }
          case _ => Unmarshal(action_input_csv_string).to[Map[String, String]]
        }

        val state_data_future: Future[Map[String, String]] = state_data_csv_string match {
          case "" => Future { Map.empty[String, String] }
          case _ => Unmarshal(state_data_csv_string).to[Map[String, String]]
        }

        val queries = Await.result(queries_future, 10.second)
        val action_input = Await.result(action_input_future, 10.second)
        val state_data = Await.result(state_data_future, 10.second)

        val document = DTDocument(state = entry(0),
          execution_order = entry(1).toInt,
          max_state_count = entry(2).toInt,
          analyzer = entry(3),
          queries = queries,
          bubble = entry(5),
          action = entry(6),
          action_input = action_input,
          state_data = state_data,
          success_value = entry(9),
          failure_value = entry(10)
        )
        document
      }
    }).toList.filter(_ != null)
    dtdocuments
  }
}
