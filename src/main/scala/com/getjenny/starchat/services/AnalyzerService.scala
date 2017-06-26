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

import scala.util.{Failure, Success, Try}
import akka.event.{Logging, LoggingAdapter}
import akka.event.Logging._
import com.getjenny.starchat.SCActorSystem
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse
import com.getjenny.pattern_extraction._

case class PatternExtractionItem(declaration: String,
                                 extractor: PatternExtraction,
                                 build: Boolean,
                                 message: String)

case class AnalyzerItem(declaration: String,
                         analyzer: StarchatAnalyzer,
                         build: Boolean)

case class DecisionTableRuntimeItem(execution_order: Int,
                                    max_state_counter: Int,
                                    analyzer: AnalyzerItem,
                                    pattern_extractor: PatternExtractionItem,
                                    queries: List[TextTerms]
                                   )

//begin
//val regex_elements = Map("regex" ->
//  """((?:[1-9]+)-(?:[0-9]+)(?: (?:[1-9]+)-(?:[0-9]+))*)""", "groups" -> "group1")
//val regex_elements = Map("regex" ->
//  """([1-9]+)-([0-9]+)""", "groups" -> "group1,group2")
//val regex_input = "111-222 333-342"
//end

object AnalyzerService {
  var analyzer_map : mutable.LinkedHashMap[String, DecisionTableRuntimeItem] =
    mutable.LinkedHashMap.empty[String, DecisionTableRuntimeItem]
}

class AnalyzerService(implicit val executionContext: ExecutionContext) {
  val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)
  val elastic_client = DecisionTableElasticClient
  val termService = new TermService
  val decisionTableService = new DecisionTableService

  def getAnalyzers: mutable.LinkedHashMap[String, DecisionTableRuntimeItem] = {
    val client: TransportClient = elastic_client.get_client()
    val qb : QueryBuilder = QueryBuilders.matchAllQuery()

    val refresh_index = elastic_client.refresh_index()
    if(refresh_index.failed_shards_n > 0) {
      throw new Exception("DecisionTable : index refresh failed: (" + elastic_client.index_name + ")")
    }

    val scroll_resp : SearchResponse = client.prepareSearch(elastic_client.index_name)
      .setTypes(elastic_client.type_name)
      .setQuery(qb)
      .setFetchSource(Array("state", "execution_order", "analyzer", "queries"), Array.empty[String])
      .setScroll(new TimeValue(60000))
      .setSize(1000).get()

    //get a map of stateId -> AnalyzerItem (only if there is smt in the field "analyzer")
    val analyzersLHM = mutable.LinkedHashMap.empty[String, DecisionTableRuntimeItem]
    val analyzers_data : List[(String, DecisionTableRuntimeItem)] = scroll_resp.getHits.getHits.toList.map({ e =>
      val item: SearchHit = e
      val state : String = item.getId
      val source : Map[String, Any] = item.getSource.asScala.toMap

      val analyzer_declaration : String = source.get("analyzer") match {
        case Some(t) => t.asInstanceOf[String]
        case None => ""
      }

      val pattern_extractor_declaration : String = source.get("pattern_extractor") match {
        case Some(t) => t.asInstanceOf[String]
        case None => ""
      }


      val execution_order : Int = source.get("execution_oder") match {
        case Some(t) => t.asInstanceOf[Int]
        case None => 0
      }

      val max_state_counter : Int = source.get("max_state_counter") match {
        case Some(t) => t.asInstanceOf[Int]
        case None => 0
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

      val decisionTableRuntimeItem: DecisionTableRuntimeItem = DecisionTableRuntimeItem(execution_order=execution_order,
        max_state_counter=max_state_counter,
        analyzer=AnalyzerItem(declaration=analyzer_declaration, build=false,
          analyzer=null),
        pattern_extractor=PatternExtractionItem(declaration=pattern_extractor_declaration, build=false,
          extractor=null, message = ""),
        queries=queries_terms)
      (state, decisionTableRuntimeItem)
    }).filter(_._2.analyzer.declaration != "").sortWith(_._2.execution_order < _._2.execution_order)

    analyzers_data.foreach(x => {
      analyzersLHM += x
    })
    analyzersLHM
  }

  def buildAnalyzers(analyzers_map: mutable.LinkedHashMap[String, DecisionTableRuntimeItem]):
                  mutable.LinkedHashMap[String, DecisionTableRuntimeItem] = {
    val result = analyzers_map.map(item => {
      val execution_order = item._2.execution_order
      val max_state_counter = item._2.max_state_counter
      val analyzer_declaration = item._2.analyzer.declaration
      val queries_terms = item._2.queries
      val analyzer : StarchatAnalyzer = if (analyzer_declaration != "") {
        new StarchatAnalyzer(analyzer_declaration)
      } else {
        null
      }

      val build = analyzer != null

      val decisionTableRuntimeItem = DecisionTableRuntimeItem(execution_order=execution_order,
        max_state_counter=max_state_counter,
        analyzer=AnalyzerItem(declaration=analyzer_declaration, build=build, analyzer=analyzer),
        pattern_extractor=item._2.pattern_extractor,
        queries=queries_terms)
      (item._1, decisionTableRuntimeItem)
    }).filter(_._2.analyzer.build)
    result
  }

  def buildPatternMatching(analyzers_map: mutable.LinkedHashMap[String, DecisionTableRuntimeItem]):
                                                      mutable.LinkedHashMap[String, DecisionTableRuntimeItem] = {
    val result = analyzers_map.map(item => {
      if (item._2.pattern_extractor.declaration != "") {
        val execution_order = item._2.execution_order
        val max_state_counter = item._2.max_state_counter
        val pattern_extractor_declaration = item._2.pattern_extractor.declaration
        val queries_terms = item._2.queries
        val analyzer = item._2.analyzer


        val (pattern_extractor : PatternExtractionRegex, message: String) = try {
          val extractor = new PatternExtractionRegex(pattern_extractor_declaration)
          val message = "no errors"
          (extractor, message)
        } catch {
          case e: PatternExtractionDeclarationParsingException =>
            val message = "Cannot parse the string with [<groups>+]|<regex>?: " + e.message
            log.error(message)
            (null, message)
          case e: PatternExtractionParsingException =>
            val message = "Cannot parse the regular expression: " + e.message
            log.error(message)
            (null, message)
          case e: PatternExtractionBadSpecificationException =>
            val message = "Bad specification of the pattern matching expression: " + e.message
            log.error(message)
            (null, message)
        }

        val build = pattern_extractor != null
        val pattern_extractor_item = PatternExtractionItem(build = build, extractor = pattern_extractor,
          declaration = pattern_extractor_declaration, message = message)

        val decisionTableRuntimeItem = DecisionTableRuntimeItem(execution_order=execution_order,
          max_state_counter=max_state_counter,
          analyzer=analyzer,
          pattern_extractor=pattern_extractor_item,
          queries=queries_terms)
        (item._1, decisionTableRuntimeItem)
      } else {
        // no Pattern matching declaration
        item
      }
    }).filter(_._2.pattern_extractor.build)
    result
  }

  def loadAnalyzer : Future[Option[DTAnalyzerLoad]] = Future {
    AnalyzerService.analyzer_map = buildAnalyzers(getAnalyzers)
    val dt_analyzer_load = DTAnalyzerLoad(num_of_entries=AnalyzerService.analyzer_map.size)
    Option {dt_analyzer_load}
  }

  def getDTAnalyzerMap : Future[Option[DTAnalyzerMap]] = {
    val analyzers = Future(Option(DTAnalyzerMap(AnalyzerService.analyzer_map.map(x => {
      val dt_analyzer = DTAnalyzerItem(x._2.analyzer.declaration, x._2.analyzer.build)
      (x._1, dt_analyzer)
    }).toMap)))
    analyzers
  }

  def evaluateAnalyzer(analyzer_request: AnalyzerEvaluateRequest):
    Future[Option[AnalyzerEvaluateResponse]] = {

    val analyzer = Try(new StarchatAnalyzer(analyzer_request.analyzer))
    val response = analyzer match {
      case Failure(exception) =>
        val analyzer_response = AnalyzerEvaluateResponse(build = false,
          value = 0.0, build_message = exception.getMessage)
        analyzer_response
      case Success(result) =>
        val eval_res = result.evaluate(analyzer_request.query)
        val analyzer_response = AnalyzerEvaluateResponse(build = true,
          value = eval_res, build_message = "success")
        analyzer_response
    }

    Future { Option { response } }
  }

  def initializeAnalyzers(): Unit = {
    if (AnalyzerService.analyzer_map == mutable.LinkedHashMap.empty[String, DecisionTableRuntimeItem]) {
      val result: Try[Option[DTAnalyzerLoad]] =
        Await.ready(loadAnalyzer, 60.seconds).value.get
      result match {
        case Success(t) => log.info("analyzers loaded: " + t.get.num_of_entries)
        case Failure(e) => log.error("can't load analyzers: " + e.toString)
      }
    } else {
      log.info("analyzers already loaded")
    }
  }

}

