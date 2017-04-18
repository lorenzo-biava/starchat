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
import java.net.URLEncoder

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import com.getjenny.starchat.serializers.JsonSupport
import scopt.OptionParser

import scala.concurrent.Await
import scala.collection.immutable
import scala.io.Source

object DeleteDecisionTable extends JsonSupport {

  private case class Params(
                             host: String = "http://localhost:8888",
                             path: String = "/decisiontable",
                             inputfile: String = "decision_table.csv",
                             skiplines: Int = 1,
                             timeout: Int = 60
                           )

  private def doDeleteDecisionTable(params: Params) {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val skiplines = params.skiplines

    val base_url = params.host + params.path
    lazy val term_text_entries = Source.fromFile(params.inputfile).getLines

    val httpHeader: immutable.Seq[HttpHeader] = immutable.Seq(RawHeader("application", "json"))
    val timeout = Duration(params.timeout, "s")

    term_text_entries.drop(skiplines).foreach(entry => {
      val cols = entry.split(",").map(_.trim)
      val numcols = cols.length
      val state = cols(0)
      val url = base_url + "/" + URLEncoder.encode(state, "UTF-8")

      val response_future: Future[HttpResponse] =
        Http().singleRequest(HttpRequest(
          method = HttpMethods.DELETE,
          uri = url,
          headers = httpHeader))
      val result = Await.result(response_future, timeout)
      result.status match {
        case StatusCodes.OK => println("deleted: " + state)
        case _ =>
          println("failed delete state(" + state + ") Row(" + entry + ") Message(" + result.toString() + ")")
      }

    })
    Await.ready(system.terminate(), Duration.Inf)
  }

  def main(args: Array[String]) {
    val defaultParams = Params()
    val parser = new OptionParser[Params]("DeleteDecisionTable") {
      head("Delete decision table items")
      help("help").text("prints this usage text")
      opt[String]("inputfile")
        .text(s"the csv file with the list of states" +
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
        .text(s"skip the first N lines from the csv file" +
          s"  default (skip csv header): ${defaultParams.skiplines}")
        .action((x, c) => c.copy(skiplines = x))
    }

    parser.parse(args, defaultParams) match {
      case Some(params) =>
        doDeleteDecisionTable(params)
      case _ =>
        sys.exit(1)
    }
  }
}
