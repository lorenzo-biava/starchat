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

case class AnalyzerItem(declaration: String,
                        build: Boolean,
                        analyzer: StarchatAnalyzer,
                        queries: List[TextTerms]
                       )

object AnalyzerService {
  var analyzer_map : Map[String, AnalyzerItem] = Map.empty[String, AnalyzerItem]
}

class AnalyzerService(implicit val executionContext: ExecutionContext) {
  val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)
  val elastic_client = DecisionTableElasticClient
  val termService = new TermService
  val decisionTableService = new DecisionTableService

  def getAnalyzers: Map[String, AnalyzerItem] = {
    val client: TransportClient = elastic_client.get_client()
    val qb : QueryBuilder = QueryBuilders.matchAllQuery()

    val refresh_index = elastic_client.refresh_index()
    if(refresh_index.failed_shards_n > 0) {
      throw new Exception("DecisionTable : index refresh failed: (" + elastic_client.index_name + ")")
    }

    val scroll_resp : SearchResponse = client.prepareSearch(elastic_client.index_name)
      .setTypes(elastic_client.type_name)
      .setQuery(qb)
      .setFetchSource(Array("state", "analyzer", "queries"), Array.empty[String])
      .setScroll(new TimeValue(60000))
      .setSize(1000).get()

    //get a map of stateId -> AnalyzerItem (only if there is smt in the field "analyzer")
    val analyzers_data : Map[String, AnalyzerItem] = scroll_resp.getHits.getHits.toList.map({ e =>
      val item: SearchHit = e
      val state : String = item.getId
      val source : Map[String, Any] = item.getSource.asScala.toMap
      val declaration : String = source.get("analyzer") match {
        case Some(t) => t.asInstanceOf[String]
        case None => ""
      }

      val queries : List[String] = source.get("queries") match {
        case Some(t) =>
          val query_array = t.asInstanceOf[java.util.ArrayList[java.util.HashMap[String, String]]].asScala.toList
            .map(q_e => q_e.get("query"))
          query_array
        case None => List[String]()
      }

      val queries_terms: List[TextTerms] = queries.map(q => {
        val query_terms = termService.textToVectors(q)
        query_terms
      }).filter(_.nonEmpty).map(x => x.get)

      val analyzerItem = AnalyzerItem(declaration, false, null, queries_terms)
      (state, analyzerItem)
    }).filter(_._2.declaration != "").toMap
    analyzers_data
  }

  def buildAnalyzers(analyzers_map: Map[String, AnalyzerItem]): Map[String, AnalyzerItem] = {
    val result = analyzers_map.map(item => {
      val declaration = item._2.declaration
      val queries_terms = item._2.queries
      val analyzer : StarchatAnalyzer = if (declaration != "") {
        new StarchatAnalyzer(declaration)
      } else {
        null
      }
      val build = analyzer != null

      val analyzerItem = AnalyzerItem(declaration, build, analyzer, queries_terms)
      (item._1, analyzerItem)
    }).filter(_._2.build)
    result
  }

  def loadAnalyzer : Future[Option[DTAnalyzerLoad]] = Future {
    AnalyzerService.analyzer_map = buildAnalyzers(getAnalyzers)
    val dt_analyzer_load = DTAnalyzerLoad(num_of_entries=AnalyzerService.analyzer_map.size)
    Option {dt_analyzer_load}
  }

  def getDTAnalyzerMap : Future[Option[DTAnalyzerMap]] = {
    val analyzers = Future(Option(DTAnalyzerMap(AnalyzerService.analyzer_map.map(x => {
      val dt_analyzer = DTAnalyzerItem(x._2.declaration, x._2.build)
      (x._1, dt_analyzer)
    }))))
    analyzers
  }

  def evaluateAnalyzer(analyzer_request: AnalyzerEvaluateRequest):
    Future[Option[AnalyzerEvaluateResponse]] = {

    val analyzer = Try(new StarchatAnalyzer(analyzer_request.analyzer))
    val response = analyzer match {
      case Failure(exception) => {
        val analyzer_response = AnalyzerEvaluateResponse(false, 0.0, exception.getMessage)
        analyzer_response
      } case Success(result) => {
        val eval_res = result.evaluate(analyzer_request.query)
        val analyzer_response = AnalyzerEvaluateResponse(true, eval_res, "success")
        analyzer_response
      }
    }

    Future { Option { response } }
  }

  def initializeAnalyzers(): Unit = {
    if (AnalyzerService.analyzer_map == Map.empty[String, AnalyzerItem]) {
      val result: Try[Option[DTAnalyzerLoad]] =
        Await.ready(loadAnalyzer, 60.seconds).value.get
      result match {
        case Success(t) => log.info("analyzers loaded")
        case Failure(e) => log.error("can't load analyzers: " + e.toString)
      }
    } else {
      log.info("analyzers already loaded")
    }
  }

}

