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
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scalaz.Scalaz._

case class ResponseServiceException(message: String = "", cause: Throwable = None.orNull)
  extends Exception(message, cause)

case class ResponseServiceDocumentNotFoundException(message: String = "", cause: Throwable = None.orNull)
  extends Exception(message, cause)

case class ResponseServiceDTNotLoadedException(message: String = "", cause: Throwable = None.orNull)
  extends Exception(message, cause)

/**
  * Implements response functionalities
  */
object ResponseService {
  private[this] val elasticClient: DecisionTableElasticClient.type = DecisionTableElasticClient
  private[this] val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)
  private[this] val termService: TermService.type = TermService
  private[this] val decisionTableService: DecisionTableService.type = DecisionTableService
  private[this] val cronReloadDTService: CronReloadDTService.type = CronReloadDTService

  def getNextResponse(indexName: String, request: ResponseRequestIn):
  Future[Option[ResponseRequestOutOperationResult]] = {

    if(! AnalyzerService.analyzersMap.contains(indexName)) {
      val message = "Decision table not ready for index(" + indexName + "), triggering reloading, please retry later"
      log.debug(message)
      cronReloadDTService.reloadAnalyzersOnce()
      throw ResponseServiceDTNotLoadedException(message)
    }

    // calculate and return the ResponseRequestOut
    val userText: String = request.user_input match {
      case Some(t) =>
        t.text.getOrElse("")
      case _ => ""
    }

    val conversationId: String = request.conversation_id

    val variables : Map[String, String] = request.values match {
      case Some(vars) => vars.data.getOrElse(Map[String, String]())
      case _ => Map.empty[String, String]
    }

    val traversedStates: List[String] = request.traversed_states.getOrElse(List.empty[String])
    val traversedStatesCount: Map[String, Int] =
      traversedStates.foldLeft(Map.empty[String, Int])((map, word) => map + (word -> (map.getOrElse(word,0) + 1)))

    // refresh last_used timestamp
    Try(AnalyzerService.analyzersMap(indexName).lastEvaluationTimestamp = System.currentTimeMillis) match {
      case Success(_) => ;
      case Failure(e) =>
        val message = "could not update the last evaluation timestamp for: " + indexName
        log.error(message)
        throw ResponseServiceException(message, e)
    }

    // prepare search result for search analyzer
    val searchResAnalyzers = decisionTableService.searchDtQueries(indexName, userText).map(searchRes => {
      val analyzersInternalData = decisionTableService.resultsToMap(searchRes)
      AnalyzersData(extracted_variables = variables, item_list = traversedStates,
        data = analyzersInternalData)
    })

    val returnValue: String = request.values match {
      case Some(t) =>
        t.return_value.getOrElse("")
      case _ => ""
    }

    val returnState: Future[Option[ResponseRequestOutOperationResult]] = searchResAnalyzers.map( data => {
      if (returnValue.nonEmpty) {
        // there is a state in return_value (eg the client asked for a state), no analyzers evaluation
        decisionTableService.read(indexName, List[String](returnValue)).map {
          case Some(searchDocRes) =>
            val doc: DTDocument = searchDocRes.hits.headOption match {
              case Some(searchRes) => searchRes.document
              case _ => throw ResponseServiceDocumentNotFoundException("Document not found: " + returnValue)
            }
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

            Some(
              ResponseRequestOutOperationResult(ReturnMessageData(200, ""), Option {
                List(responseData)
              })) // success
          case _ =>
            Some(ResponseRequestOutOperationResult(
              ReturnMessageData(500, "Error during state retrieval"), None))} // internal error
      } else {
        // No states in the return values
        val maxResults: Int = request.max_results.getOrElse(2)
        val threshold: Double = request.threshold.getOrElse(0.0d)
        val analyzerValues: Map[String, Result] =
          AnalyzerService.analyzersMap(indexName).analyzerMap
            .filter { case (_, runtimeAnalyzerItem) => runtimeAnalyzerItem.analyzer.build === true }
            .filter { case (stateName, runtimeAnalyzerItem) =>
              val traversedStateCount = traversedStatesCount.getOrElse(stateName, 0)
              val maxStateCount = runtimeAnalyzerItem.maxStateCounter
              maxStateCount === 0 ||
                traversedStateCount < maxStateCount // skip states already evaluated too much times
            }.map{ case (stateName, runtimeAnalyzerItem) =>
            val analyzerEvaluation = runtimeAnalyzerItem.analyzer.analyzer match {
              case Some(starchatAnalyzer) =>
                Try(starchatAnalyzer.evaluate(userText, data = data)) match {
                  case Success(evalRes) =>
                    log.debug("ResponseService: Evaluation of State(" +
                      stateName + ") Query(" + userText + ") Score(" + evalRes.toString + ")")
                    evalRes
                  case Failure(e) =>
                    val message = "ResponseService: Evaluation of (" + stateName + ") : " + e.getMessage
                    log.error(message)
                    throw AnalyzerEvaluationException(message, e)
                }
              case _ =>
                val message = "ResponseService: analyzer is None (" + stateName + ")"
                log.error(message)
                throw AnalyzerEvaluationException(message)
            }
            val stateId = stateName
            (stateId, analyzerEvaluation)
          }.toList
            .filter { case (_, analyzerEvaluation) => analyzerEvaluation.score > threshold }
            .sortWith {
              case ((_, analyzerEvaluation1), (_, analyzerEvaluation2)) =>
                analyzerEvaluation1.score > analyzerEvaluation2.score
            }.take(maxResults).toMap

        if (analyzerValues.nonEmpty) {
          decisionTableService.read(indexName, analyzerValues.keys.toList) map {
            case Some(docResults) =>
              val dtDocumentsList = docResults.hits.par.map(item => {
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
                    evaluationRes.data.extracted_variables
                      .filter { case (key, _) => !(key matches "\\A__temp__.*") }

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
              Some(
                ResponseRequestOutOperationResult(ReturnMessageData(200, ""), Option {
                  dtDocumentsList
                })) // success
            case _ =>
              val message = "ResponseService: could not read states: from index(" +
                indexName + ") " + analyzerValues.keys.mkString(",")
              log.error(message)
              throw AnalyzerEvaluationException(message)
          }
        } else Future {
          Some(ResponseRequestOutOperationResult(ReturnMessageData(204, ""), Option {
            List.empty[ResponseRequestOut]
          }))
        }
      }
    }).flatten
    returnState
  }
}
