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
import scala.concurrent.Await
import scala.collection.immutable
import scala.collection.immutable.{List, Map}
import java.io.{File, FileReader}

object SimilarityTest extends JsonSupport {

  private case class Params(
                            host: String = "http://localhost:8888",
                            path: String = "/analyzers_playground",
                            inputfile: String = "pairs.csv",
                            analyzer: String = "keyword(\"test\")",
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

    term_text_entries.foreach(entry => {
      val id = entry(0)
      val qid1 = entry(1)
      val qid2 = entry(2)
      val text1 = entry(3)
      val text2 = entry(4)
      val is_duplicate = entry(5)

      val analyzer: String = params.analyzer.replace("%text1", text1).replace("%text2", text2)
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
          println(id, qid1, qid2,
            "\"" + text1.replace("\"", "\"\"") + "\"",
            "\"" + text2.replace("\"", "\"\"") + "\"",
            is_duplicate, value.value)
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