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
import com.getjenny.analyzer.analyzers._
import com.getjenny.analyzer.expressions.Result
import com.getjenny.analyzer.expressions.Data

/**
  * Implements response functionalities
  */
class ResponseService(implicit val executionContext: ExecutionContext) {
  val elastic_client = DecisionTableElasticClient
  val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)
  val termService = new TermService
  val decisionTableService = new DecisionTableService

  def getNextResponse(request: ResponseRequestIn): Future[Option[ResponseRequestOutOperationResult]] = Future {
    // calculate and return the ResponseRequestOut

    val user_text: String = if (request.user_input.isDefined) {
      request.user_input.get.text.getOrElse("")
    } else {
      ""
    }

    val conversation_id: String = request.conversation_id

    val variables: Map[String, String] = if (request.values.isDefined)
      request.values.get.data.getOrElse(Map[String, String]())
    else
      Map.empty[String, String]

    val traversed_states: List[String] = request.traversed_states.getOrElse(List.empty[String])
    val traversed_states_count: Map[String, Int] =
      traversed_states.foldLeft(Map.empty[String, Int])((map, word) => map + (word -> (map.getOrElse(word,0) + 1)))

    val data: Data = Data(extracted_variables = variables, item_list = traversed_states)

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
          if (data.extracted_variables.nonEmpty) {
            for ((key, value) <- data.extracted_variables) {
              bubble = bubble.replaceAll("%" + key + "%", value)
              action_input = doc.action_input map { case (ki, vi) =>
                val new_value: String = vi.replaceAll("%" + key + "%", value)
                (ki, new_value)
              }
            }
          }

          /* we do not update the traversed_states list, if the state is requested we just return it */
          val response_data: ResponseRequestOut = ResponseRequestOut(conversation_id = conversation_id,
            state = state,
            traversed_states = traversed_states,
            max_state_count = max_state_count,
            analyzer = analyzer,
            bubble = bubble,
            action = doc.action,
            data = data.extracted_variables,
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
        val analyzer_values: Map[String, Result] =
          AnalyzerService.analyzer_map.filter(_._2.analyzer.build == true).filter(v => {
            val traversed_state_count = traversed_states_count.getOrElse(v._1, 0)
            val max_state_count = v._2.max_state_counter
            max_state_count == 0 ||
              traversed_state_count < max_state_count // skip states already evaluated too much times
          }).map(item => {
            val analyzer_evaluation = try {
              val evaluation_res = item._2.analyzer.analyzer.evaluate(user_text, data = data)
              log.debug("ResponseService: Evaluation of State(" +
                item._1 + ") Query(" + user_text + ") Score(" + evaluation_res.toString + ")")
              evaluation_res
            } catch {
              case e: Exception =>
                log.error("ResponseService: Evaluation of (" + item._1 + ") : " + e.getMessage)
                throw AnalyzerEvaluationException(e.getMessage, e)
            }
            val state_id = item._1
            (state_id, analyzer_evaluation)
        }).toList.filter(_._2.score > threshold).sortWith(_._2.score > _._2.score).take(max_results).toMap

        if(analyzer_values.nonEmpty) {
          val items: Future[Option[SearchDTDocumentsResults]] =
            decisionTableService.read(analyzer_values.keys.toList)
          val res : Option[SearchDTDocumentsResults] = Await.result(items, 60.seconds)
          val docs = res.get.hits.map(item => {
            val doc: DTDocument = item.document
            val state = doc.state
            val evaluation_res: Result = analyzer_values(state)
            val max_state_count: Int = doc.max_state_count
            val analyzer: String = doc.analyzer
            var bubble: String = doc.bubble
            var action_input: Map[String, String] = doc.action_input
            val state_data: Map[String, String] = doc.state_data

            for ((key, value) <- data.extracted_variables) {
              bubble = bubble.replaceAll("%" + key + "%", value)
              action_input = doc.action_input map { case (ki, vi) =>
                val new_value: String = vi.replaceAll("%" + key + "%", value)
                (ki, new_value)
              }
            }

            for ((key, value) <- evaluation_res.data.extracted_variables) {
              bubble = bubble.replaceAll("%" + key + "%", value)
              action_input = doc.action_input map { case (ki, vi) =>
                val new_value: String = vi.replaceAll("%" + key + "%", value)
                (ki, new_value)
              }
            }

            val cleaned_data =
              data.extracted_variables ++
                evaluation_res.data.extracted_variables.filter(item => !(item._1 matches "\\A__temp__.*"))

            val traversed_states_updated: List[String] = traversed_states ++ List(state)
            val response_item: ResponseRequestOut = ResponseRequestOut(conversation_id = conversation_id,
              state = state,
              max_state_count = max_state_count,
              traversed_states = traversed_states_updated,
              analyzer = analyzer,
              bubble = bubble,
              action = doc.action,
              data = cleaned_data,
              action_input = action_input,
              state_data = state_data,
              success_value = doc.success_value,
              failure_value = doc.failure_value,
              score = evaluation_res.score)
            response_item
          }).sortWith(_.score > _.score)
          ResponseRequestOutOperationResult(ReturnMessageData(200, ""), Option{docs}) // success
        } else {
          ResponseRequestOutOperationResult(ReturnMessageData(204, ""), Option{List.empty[ResponseRequestOut]})
        }
      }
    }
    return_state
  }
}
