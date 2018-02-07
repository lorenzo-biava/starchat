package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 01/07/16.
  */

import akka.event.{Logging, LoggingAdapter}
import com.getjenny.analyzer.expressions.{AnalyzersData, Data}
import com.getjenny.starchat.SCActorSystem
import com.getjenny.starchat.analyzer.analyzers.StarchatAnalyzer
import com.getjenny.starchat.entities._
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.unit._
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}
import org.elasticsearch.search.SearchHit

import scala.collection.JavaConverters._
import scala.collection.immutable.{List, Map}
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}
import scalaz.Scalaz._

case class AnalyzerServiceException(message: String = "", cause: Throwable = None.orNull)
  extends Exception(message, cause)

case class AnalyzerItem(declaration: String,
                        analyzer: Option[StarchatAnalyzer],
                        build: Boolean,
                        message: String)

case class DecisionTableRuntimeItem(executionOrder: Int,
                                    maxStateCounter: Int,
                                    analyzer: AnalyzerItem,
                                    queries: List[TextTerms]
                                   )

case class ActiveAnalyzers(
                            var analyzerMap : mutable.LinkedHashMap[String, DecisionTableRuntimeItem],
                            var lastEvaluationTimestamp: Long = -1,
                            var lastReloadingTimestamp: Long = -1
                          )

object AnalyzerService {

  var analyzersMap : mutable.Map[String, ActiveAnalyzers] = mutable.Map.empty[String, ActiveAnalyzers]
  val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)
  val elasticClient: DecisionTableElasticClient.type = DecisionTableElasticClient
  val termService: TermService.type = TermService
  val decisionTableService: DecisionTableService.type = DecisionTableService
  val dtReloadService: DtReloadService.type = DtReloadService

  def getIndexName(indexName: String, suffix: Option[String] = None): String = {
    indexName + "." + suffix.getOrElse(elasticClient.dtIndexSuffix)
  }

  def getAnalyzers(indexName: String): mutable.LinkedHashMap[String, DecisionTableRuntimeItem] = {
    val client: TransportClient = elasticClient.getClient()
    val qb : QueryBuilder = QueryBuilders.matchAllQuery()

    val refreshIndex = elasticClient.refreshIndex(getIndexName(indexName))
    if(refreshIndex.failed_shards_n > 0) {
      throw AnalyzerServiceException("DecisionTable : index refresh failed: (" + indexName + ")")
    }

    val scrollResp : SearchResponse = client.prepareSearch().setIndices(getIndexName(indexName))
      .setTypes(elasticClient.dtIndexSuffix)
      .setQuery(qb)
      .setFetchSource(Array("state", "execution_order", "max_state_counter",
        "analyzer", "queries"), Array.empty[String])
      .setScroll(new TimeValue(60000))
      .setSize(1000).get()

    //get a map of stateId -> AnalyzerItem (only if there is smt in the field "analyzer")
    val analyzersLHM = mutable.LinkedHashMap.empty[String, DecisionTableRuntimeItem]
    val analyzersData : List[(String, DecisionTableRuntimeItem)] = scrollResp.getHits.getHits.toList.map({ e =>
      val item: SearchHit = e
      val state : String = item.getId
      val source : Map[String, Any] = item.getSourceAsMap.asScala.toMap

      val analyzerDeclaration : String = source.get("analyzer") match {
        case Some(t) => t.asInstanceOf[String]
        case None => ""
      }

      val executionOrder : Int = source.get("execution_order") match {
        case Some(t) => t.asInstanceOf[Int]
        case None => 0
      }

      val maxStateCounter : Int = source.get("max_state_counter") match {
        case Some(t) => t.asInstanceOf[Int]
        case None => 0
      }

      val queries : List[String] = source.get("queries") match {
        case Some(t) =>
          val queryArray = t.asInstanceOf[java.util.ArrayList[java.util.HashMap[String, String]]].asScala.toList
            .map(q_e => q_e.get("query"))
          queryArray
        case None => List[String]()
      }

      val queriesTerms: List[TextTerms] = queries.map(q => {
        val queryTerms = termService.textToVectors(indexName, q)
        queryTerms
      }).filter(_.nonEmpty).map{
        case(queryTerms) => queryTerms match {
          case Some(textTerms) => textTerms
          case _ => throw AnalyzerServiceException("TextTerms cannot be empty")
        }
      }

      val decisionTableRuntimeItem: DecisionTableRuntimeItem =
        DecisionTableRuntimeItem(executionOrder=executionOrder,
          maxStateCounter=maxStateCounter,
          analyzer=AnalyzerItem(declaration=analyzerDeclaration, build=false, analyzer=None, message = "not built"),
          queries=queriesTerms)
      (state, decisionTableRuntimeItem)
    }).filter{case (_, decisionTableRuntimeItem) => decisionTableRuntimeItem
      .analyzer.declaration =/= ""}
      .sortWith{
        case ((_, decisionTableRuntimeItem1),(_, decisionTableRuntimeItem2)) =>
          decisionTableRuntimeItem1.executionOrder < decisionTableRuntimeItem2.executionOrder
      }

    analyzersData.foreach(x => {
      analyzersLHM += x
    })
    analyzersLHM
  }

  def buildAnalyzers(indexName: String, analyzersMap: mutable.LinkedHashMap[String, DecisionTableRuntimeItem]):
  mutable.LinkedHashMap[String, DecisionTableRuntimeItem] = {
    val result = analyzersMap.map{ case(stateId, runtimeItem) =>
      val executionOrder = runtimeItem.executionOrder
      val maxStateCounter = runtimeItem.maxStateCounter
      val analyzerDeclaration = runtimeItem.analyzer.declaration
      val queriesTerms = runtimeItem.queries
      val (analyzer : Option[StarchatAnalyzer], message: String) = if (analyzerDeclaration =/= "") {
        val restrictedArgs: Map[String, String] = Map("index_name" -> indexName)
        Try(new StarchatAnalyzer(analyzerDeclaration, restrictedArgs)) match {
          case Success(analyzerObject) =>
            (Some(analyzerObject), "Analyzer successfully built: " + stateId)
          case Failure(e) =>
            val msg = "Error building analyzer (" + stateId + ") declaration(" + analyzerDeclaration + "): " + e.getMessage
            log.error(msg)
            (None, msg)
        }
      } else {
        val msg = "analyzer declaration is empty"
        log.debug(msg)
        (None, msg)
      }

      val decisionTableRuntimeItem = DecisionTableRuntimeItem(executionOrder=executionOrder,
        maxStateCounter = maxStateCounter,
        analyzer =
          AnalyzerItem(
            declaration=analyzerDeclaration, build=analyzer.isDefined, analyzer=analyzer, message = message
          ),
        queries = queriesTerms)
      (stateId, decisionTableRuntimeItem)
    }.filter{case (_, decisionTableRuntimeItem) => decisionTableRuntimeItem.analyzer.build}
    result
  }

  def loadAnalyzers(indexName: String, propagate: Boolean = false) : Future[Option[DTAnalyzerLoad]] = Future {
    val analyzerMap = buildAnalyzers(indexName, getAnalyzers(indexName))
    val dtAnalyzerLoad = DTAnalyzerLoad(num_of_entries=analyzerMap.size)
    val activeAnalyzers: ActiveAnalyzers = ActiveAnalyzers(analyzerMap = analyzerMap,
      lastEvaluationTimestamp = 0, lastReloadingTimestamp = 0)
    AnalyzerService.analyzersMap(indexName) = activeAnalyzers

    if (propagate) {
      Try(dtReloadService.setDTReloadTimestamp(indexName, refresh = 1)) match {
        case Success(reloadTsFuture) =>
          reloadTsFuture.onComplete{
            case Success(dtReloadTimestamp) =>
              val ts = dtReloadTimestamp
                .getOrElse(DtReloadTimestamp(indexName, dtReloadService.DT_RELOAD_TIMESTAMP_DEFAULT))
              log.debug("setting dt reload timestamp to: " + ts.timestamp)
              activeAnalyzers.lastReloadingTimestamp = ts.timestamp
            case Failure(e) =>
              val message = "unable to set dt reload timestamp" + e.getMessage
              log.error(message)
              throw AnalyzerServiceException(message)
          }
        case Failure(e) =>
          val message = "unable to set dt reload timestamp" + e.getMessage
          log.error(message)
          throw AnalyzerServiceException(message)
      }
    }

    Option {dtAnalyzerLoad}
  }

  def getDTAnalyzerMap(indexName: String) : Future[Option[DTAnalyzerMap]] = {
    val analyzers = Future(Option(DTAnalyzerMap(AnalyzerService.analyzersMap(indexName).analyzerMap
      .map{
        case(stateName, dtRuntimeItem) =>
          val dtAnalyzer =
            DTAnalyzerItem(
              dtRuntimeItem.analyzer.declaration,
              dtRuntimeItem.analyzer.build,
              dtRuntimeItem.executionOrder
            )
          (stateName, dtAnalyzer)
      }.toMap)))
    analyzers
  }

  def evaluateAnalyzer(indexName: String, analyzerRequest: AnalyzerEvaluateRequest):
  Future[Option[AnalyzerEvaluateResponse]] = {
    val restrictedArgs: Map[String, String] = Map("index_name" -> indexName)
    val analyzer = Try(new StarchatAnalyzer(analyzerRequest.analyzer, restrictedArgs))
    val response = analyzer match {
      case Failure(exception) =>
        log.error("error during evaluation of analyzer: " + exception.getMessage)
        throw exception
      case Success(result) =>
        val dataInternal = analyzerRequest.data match {
          case Some(data) =>
            // prepare search result for search analyzer
            val analyzersInternalData =
              decisionTableService.resultsToMap(
                decisionTableService.searchDtQueries(indexName, analyzerRequest.query)
              )

            AnalyzersData(item_list = data.item_list, extracted_variables = data.extracted_variables,
              data = analyzersInternalData)
          case _ => AnalyzersData()
        }

        val evalRes = result.evaluate(analyzerRequest.query, dataInternal)
        val returnData = if(evalRes.data.extracted_variables.nonEmpty || evalRes.data.item_list.nonEmpty) {
          val dataInternal = evalRes.data
          Option { Data(item_list = dataInternal.item_list, extracted_variables =
            dataInternal.extracted_variables
          ) }
        } else {
          None: Option[Data]
        }

        val analyzerResponse = AnalyzerEvaluateResponse(build = true,
          value = evalRes.score, data = returnData, build_message = "success")
        analyzerResponse
    }

    Future { Option { response } }
  }

  def initializeAnalyzers(indexName: String): Unit = {
    if( ! AnalyzerService.analyzersMap.contains(indexName) ||
      AnalyzerService.analyzersMap(indexName).analyzerMap.isEmpty) {
      val result: Try[Option[DTAnalyzerLoad]] =
        Await.ready(loadAnalyzers(indexName), 60.seconds).value match {
          case Some(loadRes) => loadRes
          case _ => throw AnalyzerServiceException("Loading operation returned an empty result")
        }
      result match {
        case Success(t) =>
          val numOfEntries = t match {
            case Some(nOfEntries) => nOfEntries
            case _ => 0
          }
          log.info("analyzers loaded: " + numOfEntries)
        case Failure(e) =>
          log.error("can't load analyzers: " + e.toString)
      }
    } else {
      log.info("analyzers already loaded")
    }
  }

}

