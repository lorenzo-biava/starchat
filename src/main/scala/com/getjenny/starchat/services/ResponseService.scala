package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 01/07/16.
  */

import akka.actor.ActorSystem
import com.getjenny.starchat.entities._

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.collection.immutable.{List, Map}
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.action.search.{SearchRequestBuilder, SearchResponse, SearchType}
import org.elasticsearch.index.query.{BoolQueryBuilder, QueryBuilder, QueryBuilders}
import org.elasticsearch.common.unit._

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import org.elasticsearch.search.SearchHit
import com.getjenny.starchat.analyzer.analyzers._

import scala.util.{Failure, Success, Try}
import akka.event.{Logging, LoggingAdapter}
import akka.event.Logging._
import com.getjenny.starchat.SCActorSystem
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse

/**
  * Implements response functionalities
  */
class ResponseService(implicit val executionContext: ExecutionContext) {
  val elastic_client = DecisionTableElasticClient
  val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)
  val termService = new TermService
  val decisionTableService = new DecisionTableService

  def getNextResponse(request: ResponseRequestIn): Option[ResponseRequestOutOperationResult] = {
    // calculate and return the ResponseRequestOut

    val user_text: String = if (request.user_input.isDefined) {
      request.user_input.get.text.getOrElse("")
    } else {
      ""
    }

    val conversation_id: String = request.conversation_id

    val data: Map[String, String] = if (request.values.isDefined)
      request.values.get.data.getOrElse(Map[String, String]())
    else
      Map[String, String]()

    val return_value: String = if (request.values.isDefined)
      request.values.get.return_value.getOrElse("")
    else
      ""

    val return_state: Option[ResponseRequestOutOperationResult] = Option {
      if (!return_value.isEmpty) {
        // there is a state in return_value (eg the client asked for a state), no analyzers evaluation
        val state: Future[Option[SearchDTDocumentsResults]] =
          decisionTableService.read(List[String](return_value))
        val res: Option[SearchDTDocumentsResults] = Await.result(state, 60.seconds)
        if (res.get.total > 0) {
          val doc: DTDocument = res.get.hits.head.document
          val state: String = doc.state
          val max_state_count: Int = doc.max_state_count
          val analyzer: String = doc.analyzer
          var bubble: String = doc.bubble
          var action_input: Map[String, String] = doc.action_input
          val state_data: Map[String, String] = doc.state_data
          if (data.nonEmpty) {
            for ((key, value) <- data) {
              bubble = bubble.replaceAll("%" + key + "%", value)
              action_input = doc.action_input map { case (ki, vi) =>
                val new_value: String = vi.replaceAll("%" + key + "%", value)
                (ki, new_value)
              }
            }
          }

          val response_data: ResponseRequestOut = ResponseRequestOut(conversation_id = conversation_id,
            state = state,
            max_state_count = max_state_count,
            analyzer = analyzer,
            bubble = bubble,
            action = doc.action,
            data = data,
            action_input = action_input,
            state_data = state_data,
            success_value = doc.success_value,
            failure_value = doc.failure_value,
            score = 1.0d)

          val full_response: ResponseRequestOutOperationResult =
            ResponseRequestOutOperationResult(ReturnMessageData(200, ""), Option {
              List(response_data)
            }) // success
          full_response
        } else {
          val full_response: ResponseRequestOutOperationResult =
            ResponseRequestOutOperationResult(ReturnMessageData(500,
              "Error during state retrieval"), null) // internal error
          full_response
        }
      } else {
        // No states in the return values
        val max_results: Int = request.max_results.getOrElse(2)
        val threshold: Double = request.threshold.getOrElse(0.0d)
        val analyzer_values: Map[String, Double] =
          AnalyzerService.analyzer_map.filter(_._2.build == true).map(item => {
            val evaluation_score = try {
              log.info("Evaluation of (" + item._1 + ")")
              item._2.analyzer.evaluate(user_text)
            } catch {
              case e: Exception =>
                log.error("Evaluation of (" + item._1 + ") : " + e.getMessage)
                0.0
            }
            val state_id = item._1
            (state_id, evaluation_score)
        }).toList.filter(_._2 > threshold).sortWith(_._2 > _._2).take(max_results).toMap

        if(analyzer_values.nonEmpty) {
          val items: Future[Option[SearchDTDocumentsResults]] =
            decisionTableService.read(analyzer_values.keys.toList)
          val res : Option[SearchDTDocumentsResults] = Await.result(items, 60.seconds)
          val docs = res.get.hits.map(item => {
            val doc: DTDocument = item.document
            val state = doc.state
            val score: Double = analyzer_values(state)
            val max_state_count: Int = doc.max_state_count
            val analyzer: String = doc.analyzer
            var bubble: String = doc.bubble
            var action_input: Map[String, String] = doc.action_input
            val state_data: Map[String, String] = doc.state_data

            if (data.nonEmpty) {
              for ((key, value) <- data) {
                bubble = bubble.replaceAll("%" + key + "%", value)
                action_input = doc.action_input map { case (ki, vi) =>
                  val new_value: String = vi.replaceAll("%" + key + "%", value)
                  (ki, new_value)
                }
              }
            }

            val response_item: ResponseRequestOut = ResponseRequestOut(conversation_id = conversation_id,
              state = state,
              max_state_count = max_state_count,
              analyzer = analyzer,
              bubble = bubble,
              action = doc.action,
              data = data,
              action_input = action_input,
              state_data = state_data,
              success_value = doc.success_value,
              failure_value = doc.failure_value,
              score = score)
            response_item
          }).sortWith(_.score > _.score)
          ResponseRequestOutOperationResult(ReturnMessageData(200, ""), Option{docs}) // success
        } else {
          ResponseRequestOutOperationResult(ReturnMessageData(204, ""), null)  // no data
        }
      }
    }
    return_state
  }
}
