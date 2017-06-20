package com.getjenny.command

/**
  * Created by angelo on 18/04/17.
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

import com.getjenny.starchat.serializers.JsonSupport
import com.getjenny.starchat.entities._
import scopt.OptionParser
import scala.util.Try

import com.roundeights.hasher.Implicits._

import scala.concurrent.Await
import scala.collection.immutable
import scala.collection.immutable.{List, Map}
import java.util.Base64
import scala.io.Source

object IndexCorpusOnKnowledgeBase extends JsonSupport {
  private case class Params(
                             host: String = "http://localhost:8888",
                             path: String = "/knowledgebase",
                             inputfile: Option[String] = None: Option[String],
                             base64: Boolean = false,
                             separator: Char = ',',
                             skiplines: Int = 0,
                             timeout: Int = 60
                           )

  private def decodeBase64(in: String): String = {
    val decoded_bytes = Base64.getDecoder.decode(in)
    val decoded = new String(decoded_bytes, "UTF-8")
    decoded
  }

  private def doIndexCorpus(params: Params) {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val vecsize = 0
    val skiplines = params.skiplines

    val base_url = params.host + params.path
    val lines = Source.fromFile(name=params.inputfile.get).getLines.toList

    val conv_items: String => String = if (params.base64) {
      decodeBase64
    } else {
      identity
    }

    val httpHeader: immutable.Seq[HttpHeader] = immutable.Seq(RawHeader("application", "json"))
    val timeout = Duration(params.timeout, "s")

    lines.foreach(entry => {
      val document_string = conv_items(entry)
      val id: String = document_string.sha256

      val kb_document: KBDocument = KBDocument(
        id = id,
        conversation = "corpora",
        index_in_conversation = Option { -1 },
        question = document_string,
        question_scored_terms = None: Option[List[(String, Double)]],
        answer = document_string,
        answer_scored_terms = None: Option[List[(String, Double)]],
        verified = false,
        topics = None: Option[String],
        doctype = doctypes.hidden,
        state = None: Option[String],
        status = 0
      )

      val entity_future = Marshal(kb_document).to[MessageEntity]
      val entity = Await.result(entity_future, 10.second)
      val responseFuture: Future[HttpResponse] =
        Http().singleRequest(HttpRequest(
          method = HttpMethods.POST,
          uri = base_url,
          headers = httpHeader,
          entity = entity))
      val result = Await.result(responseFuture, timeout)
      result.status match {
        case StatusCodes.Created | StatusCodes.OK => println("indexed: " + kb_document.id +
          " text(" + kb_document.question + ")")
        case _ =>
          println("failed indexing entry(" + entry + ") Message(" + result.toString() + ")")
      }
    })
    Await.ready(system.terminate(), Duration.Inf)
  }

  def main(args: Array[String]) {
    val defaultParams = Params()
    val parser = new OptionParser[Params]("IndexKnowledgeBase") {
      head("Index conversations into the KnowledgeBase")
      help("help").text("prints this usage text")
      opt[String]("inputfile")
        .text(s"path of the file input file, a document per line, eventually base64 encoded" +
          s"  default: ${defaultParams.inputfile}")
        .action((x, c) => c.copy(inputfile = Option(x)))
      opt[String]("host")
        .text(s"*Chat base url" +
          s"  default: ${defaultParams.host}")
        .action((x, c) => c.copy(host = x))
      opt[String]("path")
        .text(s"the service path" +
          s"  default: ${defaultParams.path}")
        .action((x, c) => c.copy(path = x))
      opt[Int]("skiplines")
        .text(s"the number of lines to skip" +
          s"  default: ${defaultParams.skiplines}")
        .action((x, c) => c.copy(skiplines = x))
      opt[Int]("timeout")
        .text(s"the timeout in seconds of each insert operation" +
          s"  default: ${defaultParams.timeout}")
        .action((x, c) => c.copy(timeout = x))
      opt[Boolean]("base64")
        .text(s"specify if questions and answer are encoded in base 64" +
          s"  default: ${defaultParams.base64}")
        .action((x, c) => c.copy(base64 = x))
    }

    parser.parse(args, defaultParams) match {
      case Some(params) =>
        doIndexCorpus(params)
      case _ =>
        sys.exit(1)
    }
  }
}
