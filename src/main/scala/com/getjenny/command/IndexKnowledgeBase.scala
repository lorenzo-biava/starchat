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

import com.getjenny.starchat.serializers.JsonSupport
import com.getjenny.starchat.entities._
import scopt.OptionParser
import breeze.io.CSVReader
import scala.util.Try

import com.roundeights.hasher.Implicits._

import scala.concurrent.Await
import scala.collection.immutable
import scala.collection.immutable.{List, Map}
import java.io.{File, FileReader, Reader, FileInputStream, InputStreamReader}
import java.util.Base64

object IndexKnowledgeBase extends JsonSupport {
  private case class Params(
                             host: String = "http://localhost:8888",
                             path: String = "/knowledgebase",
                             questions_path: Option[String] = None: Option[String],
                             answers_path: Option[String] = None: Option[String],
                             associations_path: Option[String] = None: Option[String],
                             base64: Boolean = false,
                             separator: Char = ';',
                             timeout: Int = 60
                           )

  private def decodeBase64(in: String): String = {
    val decoded_bytes = Base64.getDecoder.decode(in)
    val decoded = new String(decoded_bytes, "UTF-8")
    decoded
  }

  private def load_data(params: Params, transform: String => String):
      List[Map[String, String]] = {
    val questions_input_stream: Reader = new InputStreamReader(new FileInputStream(params.questions_path.get), "UTF-8")
    lazy val questions_entries = CSVReader.read(input = questions_input_stream, separator = params.separator,
      quote = '"', skipLines = 0)

    val questions_map = questions_entries.zipWithIndex.map(entry => {
      if (entry._1.size < 2) {
        println("Error [questions] with line: " + entry._2)
        (entry._2, false, "", "")
      } else {
        val entry0: String = entry._1(0)
        val entry1: String = entry._1(1)
        (entry._2, true, entry0, transform(entry1))
      }
    }).filter(_._2).map(x => (x._3, x._4)).toMap

    val answers_input_stream: Reader = new InputStreamReader(new FileInputStream(params.questions_path.get), "UTF-8")
    lazy val answers_entries = CSVReader.read(input = answers_input_stream, separator = params.separator,
      quote = '"', skipLines = 0)

    val answer_map = answers_entries.zipWithIndex.map(entry => {
      if (entry._1.size < 2) {
        println("Error [answers] with line: " + entry._2)
        (entry._2, false, "", "")
      } else {
        val entry0: String = entry._1(0)
        val entry1: String = entry._1(1)
        (entry._2, true, entry0, transform(entry1))
      }
    }).filter(_._2).map(x => (x._3, x._4)).toMap

    val file_assoc = new File(params.associations_path.get)
    val file_reader_assoc = new FileReader(file_assoc)
    lazy val association_entries = CSVReader.read(input = file_reader_assoc, separator = params.separator,
      quote = '"', skipLines = 1)

    val conv_pairs = association_entries.map(entry => {
      val question_id = entry(0)
      val answer_id = entry(3)
      val question = Try(questions_map(question_id)).getOrElse("")
      val answer = Try(answer_map(answer_id)).getOrElse("")
      val val_map = Map(
        "conversation_id" -> entry(1),
        "question_id" -> question_id,
        "position" -> entry(2),
        "answer_id" -> answer_id,
        "question" -> question,
        "answer" -> answer
      )
      val_map
    })
    conv_pairs.toList
  }

  private def doIndexConversationPairs(params: Params) {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val vecsize = 0

    val base_url = params.host + params.path

    val conv_items = if (params.base64) {
      load_data(params, decodeBase64)
    } else {
      load_data(params, identity)
    }

    val httpHeader: immutable.Seq[HttpHeader] = immutable.Seq(RawHeader("application", "json"))
    val timeout = Duration(params.timeout, "s")

    conv_items.foreach(entry => {
      val id: String = entry.toString().sha256

      val kb_document: KBDocument = KBDocument(
        id = id,
        conversation = entry("conversation_id"),
        index_in_conversation =  Option { entry("position").toInt },
        question = entry("question"),
        question_scored_terms = None: Option[List[(String, Double)]],
        answer = entry("answer"),
        answer_scored_terms = None: Option[List[(String, Double)]],
        verified = false,
        topics = None: Option[String],
        doctype = doctypes.normal,
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
          " conv(" + kb_document.conversation + ")" +
          " position(" + kb_document.index_in_conversation.get + ")" +
          " q_id(" + entry("question_id") + ")" +
          " a_id(" + entry("answer_id") + ")")
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
      opt[String]("questions_path")
        .text(s"path of the file with questions, format: <question_id>;<question>" +
          s"  default: ${defaultParams.questions_path}")
        .action((x, c) => c.copy(questions_path = Option(x)))
      opt[String]("answers_path")
        .text(s"path of the file with answers, format: <answer_id>;<answer>" +
          s"  default: ${defaultParams.answers_path}")
        .action((x, c) => c.copy(answers_path = Option(x)))
      opt[String]("associations_path")
        .text(s"path of the file with answers in format: " +
            "<question_id>;<conversation_id>;<pos. in conv.>;<answer_id>" +
          s"  default: ${defaultParams.associations_path}")
        .action((x, c) => c.copy(associations_path = Option(x)))
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
      opt[Boolean]("base64")
        .text(s"specify if questions and answer are encoded in base 64" +
          s"  default: ${defaultParams.base64}")
        .action((x, c) => c.copy(base64 = x))
    }

    parser.parse(args, defaultParams) match {
      case Some(params) =>
        doIndexConversationPairs(params)
      case _ =>
        sys.exit(1)
    }
  }
}
