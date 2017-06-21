package com.getjenny.command

import akka.http.scaladsl.model.HttpRequest
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.ActorMaterializer
import akka.http.scaladsl.marshalling.Marshal

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import com.getjenny.starchat.entities.{Term, Terms}
import com.getjenny.starchat.serializers.JsonSupport
import scopt.OptionParser

import scala.concurrent.Await
import scala.collection.immutable
import scala.collection.immutable.Map
import scala.io.Source

object IndexTerms extends JsonSupport {

  private case class Params(
    host: String = "http://localhost:8888",
    path: String = "/term/index",
    inputfile: String = "vectors.txt",
    skiplines: Int = 0,
    timeout: Int = 60,
    vecsize: Int = 300
  )

  private def doIndexTerms(params: Params) {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
    
    val vecsize = params.vecsize
    val skiplines = params.skiplines

    val base_url = params.host + params.path
    lazy val term_text_entries = Source.fromFile(params.inputfile).getLines

    val httpHeader: immutable.Seq[HttpHeader] = immutable.Seq(RawHeader("application", "json"))
    val timeout = Duration(params.timeout, "s")

    (term_text_entries.drop(skiplines)).foreach(entry => {
      val splitted = entry.split(" ")
      val term_text = splitted.head
      var term_vector = Vector.empty[Double]

      try {
        term_vector = splitted.tail.map(e => e.toDouble).toVector
      } catch {
        case e : Exception => println("Error: " + e.getMessage)
      }

      if (term_vector.length != vecsize) {
        println("Error: file row does not contains a consistent vector Row(" + entry + ")")
      } else {
        val term = Term(term = term_text,
          synonyms = None: Option[Map[String, Double]],
          antonyms = None: Option[Map[String, Double]],
          tags = None: Option[String],
          features = None: Option[Map[String, String]],
          frequency_base = None: Option[Double],
          frequency_stem = None: Option[Double],
          score = None: Option[Double],
          vector = Option{term_vector})

        val terms = Terms(terms = List(term))
        val entityFuture = Marshal(terms).to[MessageEntity]
        val entity = Await.result(entityFuture, 10.second)
        val responseFuture: Future[HttpResponse] =
          Http().singleRequest(HttpRequest(
            method = HttpMethods.POST,
            uri = base_url,
            headers = httpHeader,
            entity = entity))
        val result = Await.result(responseFuture, timeout)
        result.status match {
          case StatusCodes.OK => println("indexed: " + term.term)
          case _ =>
            println("failed indexing term(" + term.term + ") Row(" + entry + ") Message(" + result.toString() + ")")
        }
      }
    })
    Await.ready(system.terminate(), Duration.Inf)
  }

  def main(args: Array[String]) {
    val defaultParams = Params()
    val parser = new OptionParser[Params]("IndexTerms") {
      head("Index vetor terms")
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
      opt[Int]("vecsize")
        .text(s"the vector size" +
          s"  default: ${defaultParams.vecsize}")
        .action((x, c) => c.copy(vecsize = x))
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
        doIndexTerms(params)
      case _ =>
        sys.exit(1)
    }
  }
}
