package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 01/07/16.
  */

import akka.event.{Logging, LoggingAdapter}
import com.getjenny.analyzer.expressions.{AnalyzersData, Data}
import com.getjenny.starchat.SCActorSystem
import com.getjenny.starchat.analyzer.analyzers.StarchatAnalyzer
import com.getjenny.starchat.entities._
import com.getjenny.starchat.services.esclient.DecisionTableElasticClient
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.unit._
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}
import org.elasticsearch.search.SearchHit

import scala.collection.JavaConverters._
import scala.collection.immutable.{List, Map}
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scalaz.Scalaz._

case class AnalyzerServiceException(message: String = "", cause: Throwable = None.orNull)
  extends Exception(message, cause)

case class AnalyzerItem(declaration: String,
                        analyzer: Option[StarchatAnalyzer],
                        build: Boolean,
                        message: String)

case class DecisionTableRuntimeItem(executionOrder: Int = -1,
                                    maxStateCounter: Int = -1,
                                    analyzer: AnalyzerItem = AnalyzerItem(
                                      declaration = "", analyzer = None, build = false, message = ""
                                    ),
                                    version: Long = -1L,
                                    queries: List[TextTerms] = List.empty[TextTerms]
                                   )

case class ActiveAnalyzers(
                            var analyzerMap : mutable.LinkedHashMap[String, DecisionTableRuntimeItem],
                            var lastEvaluationTimestamp: Long = -1L,
                            var lastReloadingTimestamp: Long = -1L
                          )

object AnalyzerService {
  var analyzersMap : mutable.Map[String, ActiveAnalyzers] = mutable.Map.empty[String, ActiveAnalyzers]
  val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)
  private[this] val elasticClient: DecisionTableElasticClient.type = DecisionTableElasticClient
  private[this] val termService: TermService.type = TermService
  private[this] val decisionTableService: DecisionTableService.type = DecisionTableService
  private[this] val dtReloadService: DtReloadService.type = DtReloadService
  val dtMaxTables: Long = elasticClient.config.getLong("es.dt_max_tables")

  private[this] def getIndexName(indexName: String, suffix: Option[String] = None): String = {
    indexName + "." + suffix.getOrElse(elasticClient.indexSuffix)
  }

  def getAnalyzers(indexName: String): mutable.LinkedHashMap[String, DecisionTableRuntimeItem] = {
    val client: TransportClient = elasticClient.client
    val qb : QueryBuilder = QueryBuilders.matchAllQuery()

    val refreshIndex = elasticClient.refresh(getIndexName(indexName))
    if(refreshIndex.failed_shards_n > 0) {
      throw AnalyzerServiceException("DecisionTable : index refresh failed: (" + indexName + ")")
    }

    val scrollResp : SearchResponse = client.prepareSearch().setIndices(getIndexName(indexName))
      .setTypes(elasticClient.indexSuffix)
      .setFetchSource(Array("state", "execution_order", "max_state_counter",
        "analyzer", "queries"), Array.empty[String])
      .setScroll(new TimeValue(60000))
      .setVersion(true)
      .setSize(1000).get()

    //get a map of stateId -> AnalyzerItem (only if there is smt in the field "analyzer")
    val analyzersLHM = mutable.LinkedHashMap.empty[String, DecisionTableRuntimeItem]
    val analyzersData : List[(String, DecisionTableRuntimeItem)] = scrollResp.getHits.getHits.toList.map({ e =>
      val item: SearchHit = e
      val state : String = item.getId
      val version : Long = item.getVersion
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
          analyzer=AnalyzerItem(declaration=analyzerDeclaration, build=false,
            analyzer = None,
            message = "Analyzer indes(" + indexName + ") state(" + state + ") not built"),
          queries = queriesTerms,
          version = version)
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

  private[this] case class BuildAnalyzerResult(analyzer : Option[StarchatAnalyzer], version: Long, message: String)

  def buildAnalyzers(indexName: String,
                     analyzersMap: mutable.LinkedHashMap[String, DecisionTableRuntimeItem],
                     incremental: Boolean = true):
  mutable.LinkedHashMap[String, DecisionTableRuntimeItem] = {
    val inPlaceIndexAnalyzers = AnalyzerService.analyzersMap.getOrElse(indexName,
      ActiveAnalyzers(mutable.LinkedHashMap.empty[String, DecisionTableRuntimeItem]))
    val result = analyzersMap.map { case(stateId, runtimeItem) =>
      val executionOrder = runtimeItem.executionOrder
      val maxStateCounter = runtimeItem.maxStateCounter
      val analyzerDeclaration = runtimeItem.analyzer.declaration
      val queriesTerms = runtimeItem.queries
      val version: Long = runtimeItem.version
      val buildAnalyzerResult: BuildAnalyzerResult =
        if (analyzerDeclaration =/= "") {
          val restrictedArgs: Map[String, String] = Map("index_name" -> indexName)
          val inPlaceAnalyzer: DecisionTableRuntimeItem =
            inPlaceIndexAnalyzers.analyzerMap.getOrElse(stateId, DecisionTableRuntimeItem())
          if(incremental && inPlaceAnalyzer.version > 0 && inPlaceAnalyzer.version === version) {
            val msg = "Analyzer already built index(" + indexName + ") state(" + stateId +
              ") version(" + version + ":" + inPlaceAnalyzer.version + ")"
            log.debug(msg)
            BuildAnalyzerResult(inPlaceAnalyzer.analyzer.analyzer, version, "Analyzer already built: " + stateId)
          } else {
            Try(new StarchatAnalyzer(analyzerDeclaration, restrictedArgs)) match {
              case Success(analyzerObject) =>
                val msg = "Analyzer successfully built index(" + indexName + ") state(" + stateId +
                  ") version(" + version + ":" + inPlaceAnalyzer.version + ")"
                log.debug(msg)
                BuildAnalyzerResult(Some(analyzerObject), version, msg)
              case Failure(e) =>
                val msg = "Error building analyzer index(" + indexName + ") state(" + stateId +
                  ") declaration(" + analyzerDeclaration +
                  ") version(" + version + ":" + inPlaceAnalyzer.version + ") : " + e.getMessage
                log.error(msg)
                BuildAnalyzerResult(None, -1L, msg)
            }
          }
        } else {
          val msg = "index(" + indexName + ") : state(" + stateId + ") : analyzer declaration is empty"
          log.debug(msg)
          BuildAnalyzerResult(None, -1L, msg)
        }

      val analyzerBuildResult = buildAnalyzerResult.analyzer match {
        case Some(_) => true
        case _ => false
      }

      val decisionTableRuntimeItem = DecisionTableRuntimeItem(executionOrder=executionOrder,
        maxStateCounter = maxStateCounter,
        analyzer =
          AnalyzerItem(
            declaration = analyzerDeclaration,
            build = analyzerBuildResult,
            analyzer = buildAnalyzerResult.analyzer,
            message = buildAnalyzerResult.message
          ),
        version = version,
        queries = queriesTerms)
      (stateId, decisionTableRuntimeItem)
    }.filter{case (_, decisionTableRuntimeItem) => decisionTableRuntimeItem.analyzer.build}
    result
  }

  def loadAnalyzers(indexName: String, incremental: Boolean = true,
                    propagate: Boolean = false) : Future[Option[DTAnalyzerLoad]] = Future {
    val analyzerMap = buildAnalyzers(indexName = indexName,
      analyzersMap = getAnalyzers(indexName), incremental = incremental)
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

    analyzer match {
      case Failure(exception) =>
        log.error("error during evaluation of analyzer: " + exception.getMessage)
        throw exception
      case Success(result) =>
        analyzerRequest.data match {
          case Some(data) =>
            // prepare search result for search analyzer
            decisionTableService.searchDtQueries(indexName, analyzerRequest.query).map(searchRes => {
              val analyzersInternalData = decisionTableService.resultsToMap(searchRes)
              val dataInternal = AnalyzersData(item_list = data.item_list,
                extracted_variables = data.extracted_variables, data = analyzersInternalData)
              val evalRes = result.evaluate(analyzerRequest.query, dataInternal)
              val returnData = if(evalRes.data.extracted_variables.nonEmpty || evalRes.data.item_list.nonEmpty) {
                val dataInternal = evalRes.data
                Some(Data(item_list = dataInternal.item_list, extracted_variables = dataInternal.extracted_variables))
              } else {
                Option.empty[Data]
              }
              Some(AnalyzerEvaluateResponse(build = true,
                value = evalRes.score, data = returnData, build_message = "success"))
            })
          case _ =>
            Future{
              Some(AnalyzerEvaluateResponse(build = true,
                value = 0.0, data = Option.empty[Data], build_message = "success"))
            }
        }
    }
  }

}

