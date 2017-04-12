package com.getjenny.command

/**
  * Created by angelo on 11/04/17.
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
import au.com.bytecode.opencsv.CSVWriter

import scala.concurrent.Await
import scala.collection.immutable
import scala.collection.immutable.{List, Map}
import java.io.{File, FileReader, FileWriter}

object SimilarityTest extends JsonSupport {

  private case class Params(
                            host: String = "http://localhost:8888",
                            path: String = "/analyzers_playground",
                            inputfile: String = "pairs.csv",
                            outputfile: String = "output.csv",
                            analyzer: String = "keyword(\"test\")",
                            text1_index: Int = 3,
                            text2_index: Int = 4,
                            separator: Char = ',',
                            skiplines: Int = 1,
                            timeout: Int = 60
                           )

  private def doCalcAnalyzer(params: Params) {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val vecsize = 0
    val skiplines = params.skiplines

    val base_url = params.host + params.path
    val file = new File(params.inputfile)
    val file_reader = new FileReader(file)
    lazy val term_text_entries = CSVReader.read(input=file_reader, separator=params.separator,
      quote = '"', skipLines=skiplines)

    val httpHeader: immutable.Seq[HttpHeader] = immutable.Seq(RawHeader("application", "json"))
    val timeout = Duration(params.timeout, "s")

    val out_file = new File(params.outputfile)
    val file_writer = new FileWriter(out_file)
    val output_csv = new CSVWriter(file_writer, params.separator, '"')

    term_text_entries.foreach(entry => {

      val text1 = entry(params.text1_index).toString
      val text2 = entry(params.text2_index).toString
      val escaped_text1 = text1.replace("\"", "\\\"")
      val escaped_text2 = text2.replace("\"", "\\\"")

      val analyzer: String =
        params.analyzer.replace("%text1", escaped_text1).replace("%text2", escaped_text2)
      val evaluate_request = AnalyzerEvaluateRequest(
        analyzer = analyzer,
        query = text2
      )

      val entity_future = Marshal(evaluate_request).to[MessageEntity]
      val entity = Await.result(entity_future, 10.second)
      val responseFuture: Future[HttpResponse] =
        Http().singleRequest(HttpRequest(
          method = HttpMethods.POST,
          uri = base_url,
          headers = httpHeader,
          entity = entity))
      val result = Await.result(responseFuture, timeout)
      result.status match {
        case StatusCodes.OK => {
          val response =
            Unmarshal(result.entity).to[AnalyzerEvaluateResponse]
          val value = response.value.get.get
          val score = value.value.toString
          val input_csv_fields = entry.toArray
          val csv_line = input_csv_fields ++ Array(score)
          output_csv.writeNext(csv_line)
        }
        case _ =>
          println("failed running analyzer(" + evaluate_request.analyzer
            + ") Query(" + evaluate_request.query + ")")
      }
    })
    Await.ready(system.terminate(), Duration.Inf)
  }

  def main(args: Array[String]) {
    val defaultParams = Params()
    val parser = new OptionParser[Params]("SimilarityTest") {
      head("Execute similarity test over a map of similar sentences." +
        " The text1 and text2 can be defined as a templates")
      help("help").text("prints this usage text")
      opt[String]("inputfile")
        .text(s"the path of the csv file with sentences" +
          s"  default: ${defaultParams.inputfile}")
        .action((x, c) => c.copy(inputfile = x))
      opt[String]("outputfile")
        .text(s"the path of the output csv file" +
          s"  default: ${defaultParams.outputfile}")
        .action((x, c) => c.copy(outputfile = x))
      opt[String]("host")
        .text(s"*Chat base url" +
          s"  default: ${defaultParams.host}")
        .action((x, c) => c.copy(host = x))
      opt[String]("analyzer")
        .text(s"the analyzer" +
          s"  default: ${defaultParams.analyzer}")
        .action((x, c) => c.copy(analyzer = x))
      opt[String]("path")
        .text(s"the service path" +
          s"  default: ${defaultParams.path}")
        .action((x, c) => c.copy(path = x))
      opt[Int]("text1_index")
        .text(s"the index of the text1 element" +
          s"  default: ${defaultParams.text1_index}")
        .action((x, c) => c.copy(text1_index = x))
       opt[Int]("text2_index")
        .text(s"the index of the text2 element" +
          s"  default: ${defaultParams.text2_index}")
        .action((x, c) => c.copy(text2_index = x))
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
        doCalcAnalyzer(params)
      case _ =>
        sys.exit(1)
    }
  }
}