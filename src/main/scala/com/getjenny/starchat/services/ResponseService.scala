package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 01/07/16.
  */

import akka.event.{Logging, LoggingAdapter}
import com.getjenny.analyzer.analyzers._
import com.getjenny.analyzer.expressions.{AnalyzersData, Result}
import com.getjenny.starchat.SCActorSystem
import com.getjenny.starchat.entities._

import scala.collection.immutable.{List, Map}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scalaz.Scalaz._

/**
  * Implements response functionalities
  */
object ResponseService {
  val elasticClient: DecisionTableElasticClient.type = DecisionTableElasticClient
  val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)
  val termService: TermService.type = TermService
  val decisionTableService: DecisionTableService.type = DecisionTableService

  def getNextResponse(indexName: String, request: ResponseRequestIn):
  Future[Option[ResponseRequestOutOperationResult]] = Future {

    if(! AnalyzerService.analyzersMap.contains(indexName)) {
      log.debug("Analyzers map for index(" + indexName + ") is not present, fetching and building")
      AnalyzerService.initializeAnalyzers(indexName)
    }

    // calculate and return the ResponseRequestOut
    val userText: String = request.user_input match {
      case Some(t) =>
        t.text.getOrElse("")
      case _ => ""
    }

    val conversationId: String = request.conversation_id

    val variables: Map[String, String] = if (request.values.isDefined)
      request.values.get.data.getOrElse(Map[String, String]())
    else
      Map.empty[String, String]

    val traversedStates: List[String] = request.traversed_states.getOrElse(List.empty[String])
    val traversedStatesCount: Map[String, Int] =
      traversedStates.foldLeft(Map.empty[String, Int])((map, word) => map + (word -> (map.getOrElse(word,0) + 1)))

    // refresh last_used timestamp
    AnalyzerService.analyzersMap(indexName).lastEvaluationTimestamp = System.currentTimeMillis

    // prepare search result for search analyzer
    val analyzersInternalData =
      decisionTableService.resultsToMap(indexName, decisionTableService.searchDtQueries(indexName, userText))

    val data: AnalyzersData = AnalyzersData(extracted_variables = variables, item_list = traversedStates,
      data = analyzersInternalData)

    val returnValue: String = request.values match {
      case Some(t) =>
        t.return_value.getOrElse("")
      case _ => ""
    }

    val returnState: Option[ResponseRequestOutOperationResult] = Option {
      if (!returnValue.isEmpty) {
        // there is a state in return_value (eg the client asked for a state), no analyzers evaluation
        val state: Future[Option[SearchDTDocumentsResults]] =
          decisionTableService.read(indexName, List[String](returnValue))
        val res: Option[SearchDTDocumentsResults] = Await.result(state, 30.seconds)
        if (res.get.total > 0) {
          val doc: DTDocument = res.get.hits.head.document
          val state: String = doc.state
          val maxStateCount: Int = doc.max_state_count
          val analyzer: String = doc.analyzer
          var bubble: String = doc.bubble
          var actionInput: Map[String, String] = doc.action_input
          val stateData: Map[String, String] = doc.state_data
          if (data.extracted_variables.nonEmpty) {
            for ((key, value) <- data.extracted_variables) {
              bubble = bubble.replaceAll("%" + key + "%", value)
              actionInput = doc.action_input map { case (ki, vi) =>
                val new_value: String = vi.replaceAll("%" + key + "%", value)
                (ki, new_value)
              }
            }
          }

          /* we do not update the traversed_states list, if the state is requested we just return it */
          val traversedStatesUpdated: List[String] = traversedStates ++ List(state)
          val responseData: ResponseRequestOut = ResponseRequestOut(conversation_id = conversationId,
            state = state,
            traversed_states = traversedStatesUpdated,
            max_state_count = maxStateCount,
            analyzer = analyzer,
            bubble = bubble,
            action = doc.action,
            data = data.extracted_variables,
            action_input = actionInput,
            state_data = stateData,
            success_value = doc.success_value,
            failure_value = doc.failure_value,
            score = 1.0d)

          val fullResponse: ResponseRequestOutOperationResult =
            ResponseRequestOutOperationResult(ReturnMessageData(200, ""), Option {
              List(responseData)
            }) // success
          fullResponse
        } else {
          val fullResponse: ResponseRequestOutOperationResult =
            ResponseRequestOutOperationResult(ReturnMessageData(500,
              "Error during state retrieval"), None) // internal error
          fullResponse
        }
      } else {
        // No states in the return values
        val maxResults: Int = request.max_results.getOrElse(2)
        val threshold: Double = request.threshold.getOrElse(0.0d)
        val analyzerValues: Map[String, Result] =
          AnalyzerService.analyzersMap(indexName).analyzerMap
            .filter{case (_, runtimeAnalyzerItem) => runtimeAnalyzerItem.analyzer.build === true}
            .filter{case (stateName, runtimeAnalyzerItem) =>
              val traversedStateCount = traversedStatesCount.getOrElse(stateName, 0)
              val maxStateCount = runtimeAnalyzerItem.maxStateCounter
              maxStateCount === 0 ||
                traversedStateCount < maxStateCount // skip states already evaluated too much times
            }.map{case (stateName, runtimeAnalyzerItem) =>
            val analyzerEvaluation = try {
              val analyzer = runtimeAnalyzerItem.analyzer.analyzer
              val evaluationRes = analyzer match {
                case Some(t) => t.evaluate(userText, data = data)
                case _ => throw AnalyzerEvaluationException("Analyzer is None")
              }
              log.debug("ResponseService: Evaluation of State(" +
                stateName + ") Query(" + userText + ") Score(" + evaluationRes.toString + ")")
              evaluationRes
            } catch {
              case e: Exception =>
                log.error("ResponseService: Evaluation of (" + stateName + ") : " + e.getMessage)
                throw AnalyzerEvaluationException(e.getMessage, e)
            }
            val stateId = stateName
            (stateId, analyzerEvaluation)
          }.toList.filter(_._2.score > threshold).sortWith(_._2.score > _._2.score).take(maxResults).toMap

        if(analyzerValues.nonEmpty) {
          val items: Future[Option[SearchDTDocumentsResults]] =
            decisionTableService.read(indexName, analyzerValues.keys.toList)
          val res : Option[SearchDTDocumentsResults] = Await.result(items, 30.seconds)
          val docs = res.get.hits.par.map(item => {
            val doc: DTDocument = item.document
            val state = doc.state
            val evaluationRes: Result = analyzerValues(state)
            val maxStateCount: Int = doc.max_state_count
            val analyzer: String = doc.analyzer
            var bubble: String = doc.bubble
            var actionInput: Map[String, String] = doc.action_input
            val stateData: Map[String, String] = doc.state_data

            for ((key, value) <- data.extracted_variables) {
              bubble = bubble.replaceAll("%" + key + "%", value)
              actionInput = doc.action_input map { case (ki, vi) =>
                val newValue: String = vi.replaceAll("%" + key + "%", value)
                (ki, newValue)
              }
            }

            for ((key, value) <- evaluationRes.data.extracted_variables) {
              bubble = bubble.replaceAll("%" + key + "%", value)
              actionInput = doc.action_input map { case (ki, vi) =>
                val newValue: String = vi.replaceAll("%" + key + "%", value)
                (ki, newValue)
              }
            }

            val cleanedData =
              data.extracted_variables ++
                evaluationRes.data.extracted_variables.filter(item => !(item._1 matches "\\A__temp__.*"))

            val traversedStatesUpdated: List[String] = traversedStates ++ List(state)
            val responseItem: ResponseRequestOut = ResponseRequestOut(conversation_id = conversationId,
              state = state,
              max_state_count = maxStateCount,
              traversed_states = traversedStatesUpdated,
              analyzer = analyzer,
              bubble = bubble,
              action = doc.action,
              data = cleanedData,
              action_input = actionInput,
              state_data = stateData,
              success_value = doc.success_value,
              failure_value = doc.failure_value,
              score = evaluationRes.score)
            responseItem
          }).toList.sortWith(_.score > _.score)
          ResponseRequestOutOperationResult(ReturnMessageData(200, ""), Option{docs}) // success
        } else {
          ResponseRequestOutOperationResult(ReturnMessageData(204, ""), Option{List.empty[ResponseRequestOut]})
        }
      }
    }
    returnState
  }
}
