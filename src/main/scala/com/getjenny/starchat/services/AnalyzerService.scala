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

import scala.collection.mutable
import scala.collection.mutable.LinkedHashMap
import scala.collection.JavaConverters._
import scala.concurrent.duration._
import org.elasticsearch.search.SearchHit
import com.getjenny.starchat.analyzer.analyzers._
import com.getjenny.analyzer.expressions.{Data, AnalyzersData}
import scala.util.{Failure, Success, Try}
import akka.event.{Logging, LoggingAdapter}
import akka.event.Logging._
import com.getjenny.starchat.SCActorSystem
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse
import com.getjenny.analyzer.expressions.Result
import scala.concurrent.ExecutionContext.Implicits.global
import scalaz._
import Scalaz._

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
                            var lastEvaluationTimestamp: Long
                          )

object AnalyzerService {

  var analyzersMap : mutable.Map[String, ActiveAnalyzers] = mutable.Map.empty[String, ActiveAnalyzers]
  val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)
  val elasticClient: DecisionTableElasticClient.type = DecisionTableElasticClient
  val termService: TermService.type = TermService
  val decisionTableService: DecisionTableService.type = DecisionTableService
  val systemService: SystemService.type = SystemService

  def getIndexName(indexName: String, suffix: Option[String] = None): String = {
    indexName + "." + suffix.getOrElse(elasticClient.dtIndexSuffix)
  }

  def getAnalyzers(indexName: String): mutable.LinkedHashMap[String, DecisionTableRuntimeItem] = {
    val client: TransportClient = elasticClient.getClient()
    val qb : QueryBuilder = QueryBuilders.matchAllQuery()

    val refreshIndex = elasticClient.refreshIndex(getIndexName(indexName))
    if(refreshIndex.failed_shards_n > 0) {
      throw new Exception("DecisionTable : index refresh failed: (" + indexName + ")")
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
      }).filter(_.nonEmpty).map(x => x.get)

      val decisionTableRuntimeItem: DecisionTableRuntimeItem =
        DecisionTableRuntimeItem(executionOrder=executionOrder,
          maxStateCounter=maxStateCounter,
          analyzer=AnalyzerItem(declaration=analyzerDeclaration, build=false, analyzer=None, message = "not built"),
          queries=queriesTerms)
      (state, decisionTableRuntimeItem)
    }).filter(_._2.analyzer.declaration != "").sortWith(_._2.executionOrder < _._2.executionOrder)

    analyzersData.foreach(x => {
      analyzersLHM += x
    })
    analyzersLHM
  }

  def buildAnalyzers(indexName: String, analyzersMap: mutable.LinkedHashMap[String, DecisionTableRuntimeItem]):
  mutable.LinkedHashMap[String, DecisionTableRuntimeItem] = {
    val result = analyzersMap.map{ case(stateId, runtimeItem) => {
      val executionOrder = runtimeItem.executionOrder
      val maxStateCounter = runtimeItem.maxStateCounter
      val analyzerDeclaration = runtimeItem.analyzer.declaration
      val queriesTerms = runtimeItem.queries
      val (analyzer : StarchatAnalyzer, message: String) = if (analyzerDeclaration != "") {
        try {
          val restrictedArgs: Map[String, String] = Map("index_name" -> indexName)
          val analyzerObject = new StarchatAnalyzer(analyzerDeclaration, restrictedArgs)
          (analyzerObject, "Analyzer successfully built: " + stateId)
        } catch {
          case e: Exception =>
            val msg = "Error building analyzer (" + stateId + ") declaration(" + analyzerDeclaration + "): " + e.getMessage
            log.error(msg)
            (None.orNull, msg)
        }
      } else {
        val msg = "analyzer declaration is empty"
        log.debug(msg)
        (None.orNull, msg)
      }

      val build = analyzer != None.orNull

      val decisionTableRuntimeItem = DecisionTableRuntimeItem(executionOrder=executionOrder,
        maxStateCounter=maxStateCounter,
        analyzer=AnalyzerItem(declaration=analyzerDeclaration, build=build, analyzer=Some(analyzer), message = message),
        queries=queriesTerms)
      (stateId, decisionTableRuntimeItem)
    }}.filter(_._2.analyzer.build)
    result
  }

  def loadAnalyzer(indexName: String, propagate: Boolean = true) : Future[Option[DTAnalyzerLoad]] = Future {
    val analyzerMap = buildAnalyzers(indexName, getAnalyzers(indexName))
    val dtAnalyzerLoad = DTAnalyzerLoad(num_of_entries=analyzerMap.size)
    val activeAnalyzers: ActiveAnalyzers = ActiveAnalyzers(analyzerMap = analyzerMap,
      lastEvaluationTimestamp = 0)
    AnalyzerService.analyzersMap(indexName) = activeAnalyzers

    if (propagate) {
      val result: Try[Option[Long]] =
        Await.ready(systemService.setDTReloadTimestamp(indexName, refresh = 1), 60.seconds).value.get
      result match {
        case Success(t) =>
          val ts: Long = t.getOrElse(0)
          log.debug("setting dt reload timestamp to: " + ts)
          SystemService.dtReloadTimestamp = ts
        case Failure(e) =>
          log.error("unable to set dt reload timestamp" + e.getMessage)
      }
    }

    Option {dtAnalyzerLoad}
  }

  def getDTAnalyzerMap(indexName: String) : Future[Option[DTAnalyzerMap]] = {
    val analyzers = Future(Option(DTAnalyzerMap(AnalyzerService.analyzersMap(indexName).analyzerMap.map(x => {
      val dtAnalyzer = DTAnalyzerItem(x._2.analyzer.declaration, x._2.analyzer.build, x._2.executionOrder)
      (x._1, dtAnalyzer)
    }).toMap)))
    analyzers
  }

  def evaluateAnalyzer(indexName: String, analyzer_request: AnalyzerEvaluateRequest): Future[Option[AnalyzerEvaluateResponse]] = {
    val restrictedArgs: Map[String, String] = Map("index_name" -> indexName)
    val analyzer = Try(new StarchatAnalyzer(analyzer_request.analyzer, restrictedArgs))
    val response = analyzer match {
      case Failure(exception) =>
        log.error("error during evaluation of analyzer: " + exception.getMessage)
        throw exception
      case Success(result) =>
        val data_internal = if (analyzer_request.data.isDefined) {
          val data = analyzer_request.data.get

          // prepare search result for search analyzer
          val analyzers_internal_data =
            decisionTableService.resultsToMap(indexName,
              decisionTableService.searchDtQueries(indexName, analyzer_request.query))

          AnalyzersData(item_list = data.item_list, extracted_variables = data.extracted_variables,
            data = analyzers_internal_data)
        } else {
          AnalyzersData()
        }

        val evalRes = result.evaluate(analyzer_request.query, data_internal)
        val returnData = if(evalRes.data.extracted_variables.nonEmpty || evalRes.data.item_list.nonEmpty) {
          val dataInternal = evalRes.data
          Option { Data(item_list = dataInternal.item_list, extracted_variables =
            dataInternal.extracted_variables
          ) }
        } else {
          None: Option[Data]
        }

        val analyzer_response = AnalyzerEvaluateResponse(build = true,
          value = evalRes.score, data = returnData, build_message = "success")
        analyzer_response
    }

    Future { Option { response } }
  }

  def initializeAnalyzers(indexName: String): Unit = {
    if( ! AnalyzerService.analyzersMap.contains(indexName) ||
        AnalyzerService.analyzersMap(indexName).analyzerMap.isEmpty) {
      val result: Try[Option[DTAnalyzerLoad]] =
        Await.ready(loadAnalyzer(indexName), 60.seconds).value.get
      result match {
        case Success(t) =>
          log.info("analyzers loaded: " + t.get.num_of_entries)
          SystemService.dtReloadTimestamp = 0
        case Failure(e) =>
          log.error("can't load analyzers: " + e.toString)
      }
    } else {
      log.info("analyzers already loaded")
    }
  }

}

