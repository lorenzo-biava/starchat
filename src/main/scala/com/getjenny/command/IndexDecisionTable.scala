package com.getjenny.command

/**
  * Created by angelo on 29/03/17.
  */

import akka.http.scaladsl.model.HttpRequest
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.ActorMaterializer
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.unmarshalling.Unmarshal

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import com.getjenny.starchat.entities._
import com.getjenny.starchat.serializers.JsonSupport
import scopt.OptionParser

import breeze.io.CSVReader

import scala.concurrent.Await
import scala.collection.immutable
import scala.collection.immutable.{List, Map}
import java.io.{File, FileReader}

object IndexDecisionTable extends JsonSupport {

  private case class Params(
                             host: String = "http://localhost:8888",
                             path: String = "/decisiontable",
                             inputfile: String = "decision_table.csv",
                             separator: Char = ',',
                             skiplines: Int = 1,
                             timeout: Int = 60,
                             numcols: Int = 11
                           )

  private def doIndexDecisionTable(params: Params) {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val vecsize = 0
    val skiplines = params.skiplines

    val base_url = params.host + params.path
    val file = new File(params.inputfile)
    val file_reader = new FileReader(file)
    lazy val file_entries = CSVReader.read(input=file_reader, separator=params.separator,
      quote = '"', skipLines=skiplines)

    val httpHeader: immutable.Seq[HttpHeader] = immutable.Seq(RawHeader("application", "json"))
    val timeout = Duration(params.timeout, "s")
    val refnumcol = params.numcols

    file_entries.foreach(entry => {
      if (entry.length != refnumcol) {
        println("Error: file row is not consistent  Row(" + entry.toString + ")")
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

        val state = DTDocument(state = entry(0),
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

        val entity_future = Marshal(state).to[MessageEntity]
        val entity = Await.result(entity_future, 10.second)
        val responseFuture: Future[HttpResponse] =
          Http().singleRequest(HttpRequest(
            method = HttpMethods.POST,
            uri = base_url,
            headers = httpHeader,
            entity = entity))
        val result = Await.result(responseFuture, timeout)
        result.status match {
          case StatusCodes.Created | StatusCodes.OK => println("indexed: " + state.state)
          case _ =>
            println("failed indexing state(" + state.state + ") Row(" + entry.toList + ") Message(" + result.toString() + ")")
        }
      }
    })
    Await.ready(system.terminate(), Duration.Inf)
  }

  def main(args: Array[String]) {
    val defaultParams = Params()
    val parser = new OptionParser[Params]("IndexDecisionTable") {
      head("Index data into DecisionTable")
      help("help").text("prints this usage text")
      opt[String]("inputfile")
        .text(s"the path of the file with the vectors" +
          s"  default: ${defaultParams.inputfile}")
        .action((x, c) => c.copy(inputfile = x))
      opt[String]("host")
        .text(s"*Chat base url" +
          s"  default: ${defaultParams.host}")
        .action((x, c) => c.copy(host = x))
      opt[String]("path")
        .text(s"the service path" +
          s"  default: ${defaultParams.path}")
        .action((x, c) => c.copy(path = x))
      opt[Int]("timeout")
        .text(s"the timeout in seconds of each insert operation" +
          s"  default: ${defaultParams.timeout}")
        .action((x, c) => c.copy(timeout = x))
      opt[Int]("skiplines")
        .text(s"skip the first N lines from vector file" +
          s"  default: ${defaultParams.skiplines}")
        .action((x, c) => c.copy(skiplines = x))
    }

    parser.parse(args, defaultParams) match {
      case Some(params) =>
        doIndexDecisionTable(params)
      case _ =>
        sys.exit(1)
    }
  }
}