package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 01/07/16.
  */

import com.getjenny.starchat.entities._
import scala.concurrent.{ExecutionContext, Future, Promise, Await}
import scala.util.{Success, Failure}
import scala.collection.immutable.{List, Map}

import org.elasticsearch.common.xcontent.XContentBuilder
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.xcontent.XContentFactory._
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.action.update.UpdateResponse
import org.elasticsearch.action.delete.DeleteResponse
import org.elasticsearch.action.get.{GetResponse, MultiGetItemResponse, MultiGetRequestBuilder, MultiGetResponse}
import org.elasticsearch.action.search.{SearchRequestBuilder, SearchResponse, SearchType}
import org.elasticsearch.index.query.{BoolQueryBuilder, QueryBuilders}
import scala.collection.JavaConverters._
import scala.concurrent.duration._
import org.elasticsearch.search.SearchHit

/**
  * Implements functions, eventually used by DecisionTableResource, for searching, get next response etc
  */
class DecisionTableService(implicit val executionContext: ExecutionContext) {
  val elastic_client = DTElasticClient

  def getNextResponse(request: ResponseRequestIn): Option[ResponseRequestOutOperationResult] = {
    // calculate and return the ResponseRequestOut

    val user_text: String = if(request.user_input.isDefined) request.user_input.get.text.getOrElse("") else ""
    val data: Map[String, String] = if(request.values.isDefined)
      request.values.get.data.getOrElse(Map[String,String]()) else Map[String,String]()
    val return_value: String =  if(request.values.isDefined) request.values.get.return_value.getOrElse("") else ""

    val return_state : Option[ResponseRequestOutOperationResult] = Option {
      return_value != "" match {
        case true => // there is a state in return_value
          val state: Future[Option[SearchDTDocumentsResults]] = read(List[String](return_value))
          val res : Option[SearchDTDocumentsResults] = Await.result(state, 30.seconds)
          if (res.get.total > 0) {
            val doc : DTDocument = res.get.hits.head.document
            var bubble : String = doc.bubble
            var action_input : Map[String,String] = doc.action_input
            if (data.nonEmpty) {
              for ((key,value) <- data) {
                bubble = bubble.replaceAll("%" + key + "%", value)
                action_input = doc.action_input map {case (ki, vi) =>
                  val new_value : String = vi.replaceAll("%" + key + "%", value)
                  (ki, new_value)
                }
              }
            }

            val response_data : ResponseRequestOut = ResponseRequestOut(bubble = bubble,
              action = doc.action,
              data = data,
              action_input = action_input,
              success_value = doc.success_value,
              failure_value = doc.failure_value)

            val full_response : ResponseRequestOutOperationResult =
                ResponseRequestOutOperationResult(ReturnMessageData(200, ""), Option{response_data}) // success
            full_response
          } else {
            val full_response : ResponseRequestOutOperationResult =
              ResponseRequestOutOperationResult(ReturnMessageData(500,
                "Error during state retrieval"), null) // internal error
            full_response
          }
        case false => // No states in the return values
          val dtDocumentSearch : DTDocumentSearch =
            DTDocumentSearch(from = Option{0}, size = Option{1}, min_score = Option{elastic_client.query_min_threshold},
              state = Option{null}, queries = Option{user_text})
          val state: Future[Option[SearchDTDocumentsResults]] = search(dtDocumentSearch)
          // search the state with the closest query value, then return that state
          val res : Option[SearchDTDocumentsResults] = Await.result(state, 30.seconds)
          if (res.get.total > 0) {
            val doc : DTDocument = res.get.hits.head.document
            var bubble : String = doc.bubble
            var action_input : Map[String,String] = doc.action_input
            if (data.nonEmpty) {
              for ((key,value) <- data) {
                bubble = bubble.replaceAll("%" + key + "%", value)
                action_input = doc.action_input map {case (ki, vi) =>
                  val new_value : String = vi.replaceAll("%" + key + "%", value)
                  (ki, new_value)
                }
              }
            }

            val response_data : ResponseRequestOut = ResponseRequestOut(bubble = bubble,
              action = doc.action,
              data = data,
              action_input = action_input,
              success_value = doc.success_value,
              failure_value = doc.failure_value)

            val full_response : ResponseRequestOutOperationResult =
              ResponseRequestOutOperationResult(ReturnMessageData(200, ""), Option{response_data}) // success
            full_response
          } else {
            val full_response : ResponseRequestOutOperationResult =
              ResponseRequestOutOperationResult(ReturnMessageData(204, ""), null)  // no data
            full_response
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

    search_builder.setMinScore(documentSearch.min_score.getOrElse(
      Option{elastic_client.query_min_threshold}.getOrElse(0.0f))
    )

    val bool_query_builder : BoolQueryBuilder = QueryBuilders.boolQuery()
    if (documentSearch.state.isDefined)
      bool_query_builder.must(QueryBuilders.termQuery("state", documentSearch.state.get))

    if(documentSearch.queries.isDefined)
      bool_query_builder.must(QueryBuilders.matchQuery("queries.stem_lmd", documentSearch.queries.get))

    search_builder.setQuery(bool_query_builder)

    val search_response : SearchResponse = search_builder
      .setFrom(documentSearch.from.getOrElse(0)).setSize(documentSearch.size.getOrElse(10))
      .execute()
      .actionGet()

    val documents : Option[List[SearchDTDocument]] = Option { search_response.getHits.getHits.toList.map( { case(e) =>

      val item: SearchHit = e

      val state : String = item.getId

      val source : Map[String, Any] = item.getSource.asScala.toMap

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

      val success_value : String = source.get("success_value") match {
        case Some(t) => t.asInstanceOf[String]
        case None => ""
      }

      val failure_value : String = source.get("failure_value") match {
        case Some(t) => t.asInstanceOf[String]
        case None => ""
      }

      val document : DTDocument = DTDocument(state = state, queries = queries, bubble = bubble,
        action = action, action_input = action_input, success_value = success_value, failure_value = failure_value)

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

  def create(document: DTDocument): Future[Option[IndexDocumentResult]] = Future {
    val builder : XContentBuilder = jsonBuilder().startObject()

    builder.field("state", document.state)
    builder.array("queries", document.queries:_*)
    builder.field("bubble", document.bubble)
    builder.field("action", document.action)

    val action_input_builder : XContentBuilder = builder.startObject("action_input")
    for ((k,v) <- document.action_input) action_input_builder.field(k,v)
    action_input_builder.endObject()

    builder.field("success_value", document.success_value)
    builder.field("failure_value", document.failure_value)

    builder.endObject()

    val json: String = builder.string()
    val client: TransportClient = elastic_client.get_client()
    val response: IndexResponse = client.prepareIndex(elastic_client.index_name, elastic_client.type_name,
                                                          document.state)
      .setSource(json)
      .get()

    val doc_result: IndexDocumentResult = IndexDocumentResult(index = response.getIndex,
      dtype = response.getType,
      id = response.getId,
      version = response.getVersion,
      created = response.isCreated
    )

    Option {doc_result}
  }

  def update(id: String, document: DTDocumentUpdate): Future[Option[UpdateDocumentResult]] = Future {
    val builder : XContentBuilder = jsonBuilder().startObject()

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

    val doc_result: UpdateDocumentResult = UpdateDocumentResult(index = response.getIndex,
      dtype = response.getType,
      id = response.getId,
      version = response.getVersion,
      created = response.isCreated
    )

    Option {doc_result}
  }

  def delete(id: String): Future[Option[DeleteDocumentResult]] = Future {
    val client: TransportClient = elastic_client.get_client()
    val response: DeleteResponse = client.prepareDelete(elastic_client.index_name, elastic_client.type_name, id).get()

    val doc_result: DeleteDocumentResult = DeleteDocumentResult(index = response.getIndex,
      dtype = response.getType,
      id = response.getId,
      version = response.getVersion,
      found = response.isFound
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

      val success_value : String = source.get("success_value") match {
        case Some(t) => t.asInstanceOf[String]
        case None => ""
      }

      val failure_value : String = source.get("failure_value") match {
        case Some(t) => t.asInstanceOf[String]
        case None => ""
      }

      val document : DTDocument = DTDocument(state = state, queries = queries, bubble = bubble,
        action = action, action_input = action_input, success_value = success_value, failure_value = failure_value)

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
