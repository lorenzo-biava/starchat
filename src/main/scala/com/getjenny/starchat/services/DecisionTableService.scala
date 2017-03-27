package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 01/07/16.
  */

import akka.actor.ActorSystem
import com.getjenny.starchat.entities._

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.collection.immutable.{List, Map}
import org.elasticsearch.common.xcontent.XContentBuilder
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.xcontent.XContentFactory._
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.action.update.UpdateResponse
import org.elasticsearch.action.delete.DeleteResponse
import org.elasticsearch.action.get.{GetResponse, MultiGetItemResponse, MultiGetRequestBuilder, MultiGetResponse}
import org.elasticsearch.action.search.{SearchRequestBuilder, SearchResponse, SearchType}
import org.elasticsearch.index.query.{BoolQueryBuilder, QueryBuilder, QueryBuilders}
import org.elasticsearch.common.unit._

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import org.elasticsearch.search.SearchHit
import org.elasticsearch.rest.RestStatus
import com.getjenny.starchat.analyzer.analyzers._

import scala.util.{Failure, Success, Try}
import akka.event.{Logging, LoggingAdapter}
import akka.event.Logging._
import com.getjenny.starchat.SCActorSystem
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse

/**
  * Implements functions, eventually used by DecisionTableResource, for searching, get next response etc
  */
class DecisionTableService(implicit val executionContext: ExecutionContext) {
  val elastic_client = DecisionTableElasticClient
  val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)

  case class AnalyzerItem(declaration: String, build: Boolean, analyzer: StarchatAnalyzer)

  var analyzer_map : Map[String, AnalyzerItem] =  Map.empty[String, AnalyzerItem]

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
      .setFetchSource(Array("state", "analyzer"), Array.empty[String])
      .setScroll(new TimeValue(60000))
      .setSize(1000).get()

    //get a map of stateId -> AnalyzerItem (only if there is smt in the field "analyzer")
    val results : Map[String, AnalyzerItem] = scroll_resp.getHits.getHits.toList.map({ e =>
      val item: SearchHit = e
      val state : String = item.getId
      val source : Map[String, Any] = item.getSource.asScala.toMap
      val declaration : String = source.get("analyzer") match {
        case Some(t) => t.asInstanceOf[String]
        case None => ""
      }

      val analyzer : StarchatAnalyzer = if (declaration != "") {
        new StarchatAnalyzer(declaration)
      } else {
        null
      }

      val build = analyzer != null

      val analyzerItem = AnalyzerItem(declaration, build, analyzer)
      (state, analyzerItem)
    }).filter(_._2.declaration != "").toMap
    results
  }

  def loadAnalyzer : Future[Option[DTAnalyzerLoad]] = Future {
    analyzer_map = getAnalyzers
    val dt_analyzer_load = DTAnalyzerLoad(num_of_entries=analyzer_map.size)
    Option {dt_analyzer_load}
  }

  def getDTAnalyzerMap : Future[Option[DTAnalyzerMap]] = {
    val analyzers = Future(Option(DTAnalyzerMap(analyzer_map.map(x => {
      val dt_analyzer = DTAnalyzerItem(x._2.declaration, x._2.build)
      (x._1, dt_analyzer)
    }))))
    analyzers
  }

  def initializeAnalyzers(): Unit = {
    val result: Try[Option[DTAnalyzerLoad]] =
      Await.ready(loadAnalyzer, 30.seconds).value.get
    result match {
      case Success(t) => log.info("analyzers loaded")
      case Failure(e) => log.error("can't load analyzers")
    }
  }

  initializeAnalyzers() // load analyzer map on startup

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
        val state: Future[Option[SearchDTDocumentsResults]] = read(List[String](return_value))
        val res: Option[SearchDTDocumentsResults] = Await.result(state, 30.seconds)
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
        val analyzer_values: Map[String, Double] = analyzer_map.filter(_._2.build == true).map(item => {
          val evaluation_score = item._2.analyzer.evaluate(user_text)
          val state_id = item._1
          (state_id, evaluation_score)
        }).toList.filter(_._2 > threshold).sortWith(_._2 > _._2).take(max_results).toMap

        if(analyzer_values.nonEmpty) {
          val items: Future[Option[SearchDTDocumentsResults]] = read(analyzer_values.keys.toList)
          val res : Option[SearchDTDocumentsResults] = Await.result(items, 30.seconds)
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

  def search(documentSearch: DTDocumentSearch): Future[Option[SearchDTDocumentsResults]] = {
    val client: TransportClient = elastic_client.get_client()
    val search_builder : SearchRequestBuilder = client.prepareSearch(elastic_client.index_name)
      .setTypes(elastic_client.type_name)
      .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)

    val min_score = documentSearch.min_score.getOrElse(
      Option{elastic_client.query_min_threshold}.getOrElse(0.0f)
    )

    val boost_exact_match_factor = documentSearch.boost_exact_match_factor.getOrElse(
      Option{elastic_client.boost_exact_match_factor}.getOrElse(1.0f)
    )

    search_builder.setMinScore(min_score)

    val bool_query_builder : BoolQueryBuilder = QueryBuilders.boolQuery()
    if (documentSearch.state.isDefined)
      bool_query_builder.must(QueryBuilders.termQuery("state", documentSearch.state.get))

    if(documentSearch.queries.isDefined) {
      bool_query_builder.must(QueryBuilders.matchQuery("queries.stem_bm25", documentSearch.queries.get))
      bool_query_builder.should(
        QueryBuilders.matchPhraseQuery("queries.raw", documentSearch.queries.get)
          .boost(1 + (min_score * boost_exact_match_factor))
      )
    }

    search_builder.setQuery(bool_query_builder)

    val search_response : SearchResponse = search_builder
      .setFrom(documentSearch.from.getOrElse(0)).setSize(documentSearch.size.getOrElse(10))
      .execute()
      .actionGet()

    val documents : Option[List[SearchDTDocument]] =
      Option { search_response.getHits.getHits.toList.map( { case(e) =>

      val item: SearchHit = e

      val state : String = item.getId

      val source : Map[String, Any] = item.getSource.asScala.toMap

      val max_state_count : Int = source.get("max_state_count") match {
        case Some(t) => t.asInstanceOf[Int]
        case None => 0
      }

      val analyzer : String = source.get("analyzer") match {
        case Some(t) => t.asInstanceOf[String]
        case None => ""
      }

      val queries : List[String] = source.get("queries") match {
        case Some(t) => t.asInstanceOf[java.util.ArrayList[String]].asScala.toList
        case None => List[String]()
      }

      val bubble : String = source.get("bubble") match {
        case Some(t) => t.asInstanceOf[String]
        case None => ""
      }

      val action : String = source.get("action") match {
        case Some(t) => t.asInstanceOf[String]
        case None => ""
      }

      val action_input : Map[String,String] = source.get("action_input") match {
        case Some(t) => t.asInstanceOf[java.util.HashMap[String,String]].asScala.toMap
        case None => Map[String, String]()
      }

      val state_data : Map[String,String] = source.get("state_data") match {
        case Some(t) => t.asInstanceOf[java.util.HashMap[String,String]].asScala.toMap
        case None => Map[String, String]()
      }

      val success_value : String = source.get("success_value") match {
        case Some(t) => t.asInstanceOf[String]
        case None => ""
      }

      val failure_value : String = source.get("failure_value") match {
        case Some(t) => t.asInstanceOf[String]
        case None => ""
      }

      val document : DTDocument = DTDocument(state = state, max_state_count = max_state_count,
        analyzer = analyzer, queries = queries, bubble = bubble,
        action = action, action_input = action_input, state_data = state_data,
        success_value = success_value, failure_value = failure_value)

      val search_document : SearchDTDocument = SearchDTDocument(score = item.score, document = document)
      search_document
    }) }

    val filtered_doc : List[SearchDTDocument] = documents.getOrElse(List[SearchDTDocument]())

    val max_score : Float = search_response.getHits.getMaxScore
    val total : Int = filtered_doc.length
    val search_results : SearchDTDocumentsResults = SearchDTDocumentsResults(total = total, max_score = max_score,
      hits = filtered_doc)

    val search_results_option : Future[Option[SearchDTDocumentsResults]] = Future { Option { search_results } }
    search_results_option
  }

  def create(document: DTDocument, refresh: Int): Future[Option[IndexDocumentResult]] = Future {
    val builder : XContentBuilder = jsonBuilder().startObject()

    builder.field("state", document.state)
    builder.field("max_state_count", document.max_state_count)
    builder.field("analyzer", document.analyzer)
    builder.array("queries", document.queries:_*)
    builder.field("bubble", document.bubble)
    builder.field("action", document.action)

    val action_input_builder : XContentBuilder = builder.startObject("action_input")
    for ((k,v) <- document.action_input) action_input_builder.field(k,v)
    action_input_builder.endObject()

    val state_data_builder : XContentBuilder = builder.startObject("state_data")
    for ((k,v) <- document.state_data) state_data_builder.field(k,v)
    state_data_builder.endObject()

    builder.field("success_value", document.success_value)
    builder.field("failure_value", document.failure_value)

    builder.endObject()

    val json: String = builder.string()
    val client: TransportClient = elastic_client.get_client()
    val response = client.prepareIndex(elastic_client.index_name,
      elastic_client.type_name, document.state).setSource(json).get()

    if (refresh != 0) {
      val refresh_index = elastic_client.refresh_index()
      if(refresh_index.failed_shards_n > 0) {
        throw new Exception("DecisionTable : index refresh failed: (" + elastic_client.index_name + ")")
      }
    }

    val doc_result: IndexDocumentResult = IndexDocumentResult(index = response.getIndex,
      dtype = response.getType,
      id = response.getId,
      version = response.getVersion,
      created = response.status == RestStatus.CREATED
    )

    Option {doc_result}
  }

  def update(id: String, document: DTDocumentUpdate, refresh: Int): Future[Option[UpdateDocumentResult]] = Future {
    val builder : XContentBuilder = jsonBuilder().startObject()

    document.analyzer match {
      case Some(t) => builder.field("analyzer", t)
      case None => ;
    }
    document.max_state_count match {
      case Some(t) => builder.field("max_state_count", t)
      case None => ;
    }
    document.queries match {
      case Some(t) =>
        builder.array("queries", t:_*)
      case None => ;
    }
    document.bubble match {
      case Some(t) => builder.field("bubble", t)
      case None => ;
    }
    document.action match {
      case Some(t) => builder.field("action", t)
      case None => ;
    }
    document.action_input match {
      case Some(t) =>
        val action_input_builder : XContentBuilder = builder.startObject("action_input")
        for ((k,v) <- t) action_input_builder.field(k,v)
        action_input_builder.endObject()
      case None => ;
    }
    document.state_data match {
      case Some(t) =>
        val state_data_builder : XContentBuilder = builder.startObject("state_data")
        for ((k,v) <- t) state_data_builder.field(k,v)
        state_data_builder.endObject()
      case None => ;
    }
    document.success_value match {
      case Some(t) => builder.field("success_value", t)
      case None => ;
    }
    document.failure_value match {
      case Some(t) => builder.field("failure_value", t)
      case None => ;
    }
    builder.endObject()

    val client: TransportClient = elastic_client.get_client()
    val response: UpdateResponse = client.prepareUpdate(elastic_client.index_name, elastic_client.type_name, id)
      .setDoc(builder)
      .get()

    if (refresh != 0) {
      val refresh_index = elastic_client.refresh_index()
      if(refresh_index.failed_shards_n > 0) {
        throw new Exception("DecisionTable : index refresh failed: (" + elastic_client.index_name + ")")
      }
    }

    val doc_result: UpdateDocumentResult = UpdateDocumentResult(index = response.getIndex,
      dtype = response.getType,
      id = response.getId,
      version = response.getVersion,
      created = response.status == RestStatus.CREATED
    )

    Option {doc_result}
  }

  def delete(id: String, refresh: Int): Future[Option[DeleteDocumentResult]] = Future {
    val client: TransportClient = elastic_client.get_client()
    val response: DeleteResponse = client.prepareDelete(elastic_client.index_name, elastic_client.type_name, id).get()

    if (refresh != 0) {
      val refresh_index = elastic_client.refresh_index()
      if(refresh_index.failed_shards_n > 0) {
        throw new Exception("DecisionTable : index refresh failed: (" + elastic_client.index_name + ")")
      }
    }

    val doc_result: DeleteDocumentResult = DeleteDocumentResult(index = response.getIndex,
      dtype = response.getType,
      id = response.getId,
      version = response.getVersion,
      found = response.status != RestStatus.NOT_FOUND
    )

    Option {doc_result}
  }

  def read(ids: List[String]): Future[Option[SearchDTDocumentsResults]] = {
    val client: TransportClient = elastic_client.get_client()
    val multiget_builder: MultiGetRequestBuilder = client.prepareMultiGet()
    multiget_builder.add(elastic_client.index_name, elastic_client.type_name, ids:_*)
    val response: MultiGetResponse = multiget_builder.get()

    val documents : Option[List[SearchDTDocument]] = Option { response.getResponses
      .toList.filter((p: MultiGetItemResponse) => p.getResponse.isExists).map( { case(e) =>

      val item: GetResponse = e.getResponse

      val state : String = item.getId

      val source : Map[String, Any] = item.getSource.asScala.toMap

      val max_state_count : Int = source.get("max_state_count") match {
        case Some(t) => t.asInstanceOf[Int]
        case None => 0
      }

      val analyzer : String = source.get("analyzer") match {
        case Some(t) => t.asInstanceOf[String]
        case None => ""
      }

      val queries : List[String] = source.get("queries") match {
        case Some(t) => t.asInstanceOf[java.util.ArrayList[String]].asScala.toList
        case None => List[String]()
      }

      val bubble : String = source.get("bubble") match {
        case Some(t) => t.asInstanceOf[String]
        case None => ""
      }

      val action : String = source.get("action") match {
        case Some(t) => t.asInstanceOf[String]
        case None => ""
      }

      val action_input : Map[String,String] = source.get("action_input") match {
        case Some(t) => t.asInstanceOf[java.util.HashMap[String,String]].asScala.toMap
        case None => Map[String,String]()
      }

      val state_data : Map[String,String] = source.get("state_data") match {
        case Some(t) => t.asInstanceOf[java.util.HashMap[String,String]].asScala.toMap
        case None => Map[String,String]()
      }

      val success_value : String = source.get("success_value") match {
        case Some(t) => t.asInstanceOf[String]
        case None => ""
      }

      val failure_value : String = source.get("failure_value") match {
        case Some(t) => t.asInstanceOf[String]
        case None => ""
      }

      val document : DTDocument = DTDocument(state = state, max_state_count = max_state_count,
        analyzer = analyzer, queries = queries, bubble = bubble,
        action = action, action_input = action_input, state_data = state_data,
        success_value = success_value, failure_value = failure_value)

      val search_document : SearchDTDocument = SearchDTDocument(score = .0f, document = document)
      search_document
    }) }

    val filtered_doc : List[SearchDTDocument] = documents.getOrElse(List[SearchDTDocument]())

    val max_score : Float = .0f
    val total : Int = filtered_doc.length
    val search_results : SearchDTDocumentsResults = SearchDTDocumentsResults(total = total, max_score = max_score,
      hits = filtered_doc)

    val search_results_option : Future[Option[SearchDTDocumentsResults]] = Future { Option { search_results } }
    search_results_option
  }
}