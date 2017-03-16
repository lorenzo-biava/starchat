package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 10/03/17.
  */

import org.elasticsearch.action.bulk._
import com.getjenny.starchat.entities._
import org.elasticsearch.action.delete.{DeleteRequestBuilder, DeleteResponse}
import org.elasticsearch.common.xcontent.XContentBuilder

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.collection.immutable.{List, Map}
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.xcontent.XContentFactory._
import org.elasticsearch.action.get.{GetResponse, MultiGetItemResponse, MultiGetRequestBuilder, MultiGetResponse}
import org.elasticsearch.action.search.{SearchRequestBuilder, SearchResponse, SearchType}
import org.elasticsearch.index.query.{BoolQueryBuilder, QueryBuilders}

import scala.collection.JavaConverters._
import org.elasticsearch.rest.RestStatus
import org.elasticsearch.search.SearchHit
import akka.event.{Logging, LoggingAdapter}
import akka.event.Logging._
import com.getjenny.starchat.SCActorSystem

/**
  * Implements functions, eventually used by TermResource
  */
class TermService(implicit val executionContext: ExecutionContext) {
  val elastic_client = IndexManagementClient
  val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)

  def vector2string(vector: Vector[Double]): String = {
    vector.zipWithIndex.map(x => x._2.toString + "|" + x._1.toString).mkString(" ")
  }

  def index_term(terms: Terms) : Option[IndexDocumentListResult] = {
    val client: TransportClient = elastic_client.get_client()

    val bulkRequest : BulkRequestBuilder = client.prepareBulk()

    terms.terms.foreach( term => {
      val builder : XContentBuilder = jsonBuilder().startObject()
      builder.field("term", term.term)
      term.synonyms match {
        case Some(t) => builder.field("synonyms", t)
        case None => ;
      }
      term.antonyms match {
        case Some(t) => builder.field("antonyms", t)
        case None => ;
      }
      term.frequency match {
        case Some(t) => builder.field("frequency", t)
        case None => ;
      }
      term.vector match {
        case Some(t) =>
          val indexable_vector: String = vector2string(t)
          builder.field("vector", indexable_vector)
        case None => ;
      }
      builder.endObject()

      bulkRequest.add(client.prepareIndex(elastic_client.index_name, elastic_client.term_type_name, term.term)
        .setSource(builder))
    })

    val bulkResponse: BulkResponse = bulkRequest.get()

    val list_of_doc_res: List[IndexDocumentResult] = bulkResponse.getItems.map(x => {
      IndexDocumentResult(x.getIndex, x.getType, x.getId,
      x.getVersion,
      x.status == RestStatus.CREATED)
    }).toList

    val result: IndexDocumentListResult = IndexDocumentListResult(list_of_doc_res)
    Option {
      result
    }
  }

  def get_term(terms_request: TermIdsRequest) : Option[Terms] = {
    val client: TransportClient = elastic_client.get_client()
    val multiget_builder: MultiGetRequestBuilder = client.prepareMultiGet()
    multiget_builder.add(elastic_client.index_name, elastic_client.term_type_name, terms_request.ids:_*)
    val response: MultiGetResponse = multiget_builder.get()
    val documents : List[Term] = response.getResponses.toList
        .filter((p: MultiGetItemResponse) => p.getResponse.isExists).map({ case(e) =>
      val item: GetResponse = e.getResponse
      val source : Map[String, Any] = item.getSource.asScala.toMap

      val term : String = source.get("term").asInstanceOf[String]

      val synonyms: Option[String] = source.get("synonyms") match {
          case Some(t) => Option{t.asInstanceOf[String]}
          case None => None: Option[String]
      }

      val antonyms : Option[String] = source.get("antonyms") match {
          case Some(t) => Option {t.asInstanceOf[String]}
          case None => None: Option[String]
      }

      val frequency : Option[Double] = source.get("antonyms") match {
        case Some(t) => Option {t.asInstanceOf[Double]}
        case None => None: Option[Double]
      }

      val vector : Option[String] = source.get("antonyms") match {
          case Some(t) => Option {t.asInstanceOf[String]}
          case None => None: Option[String]
      }

      Term(term = term, //TODO: to complete
        synonyms = Option{Map.empty[String, Double]},
        antonyms = Option{Map.empty[String, Double]},
        frequency = frequency,
        vector = None: Option[Vector[Double]],
        score = None: Option[Double])
    })

    Option {Terms(terms=documents)}
  }

  def update_term(terms: Terms) : Option[UpdateDocumentListResult] = {
    val client: TransportClient = elastic_client.get_client()

    val bulkRequest : BulkRequestBuilder = client.prepareBulk()

    terms.terms.foreach( term => {
      val builder : XContentBuilder = jsonBuilder().startObject()
      builder.field("term", term.term)
      term.synonyms match {
        case Some(t) => builder.field("synonyms", t)
        case None => ;
      }
      term.antonyms match {
        case Some(t) => builder.field("antonyms", t)
        case None => ;
      }
      term.frequency match {
        case Some(t) => builder.field("frequency", t)
        case None => ;
      }
      term.vector match {
        case Some(t) =>
          val indexable_vector: String = vector2string(t)
          builder.field("vector", indexable_vector)
        case None => ;
      }
      builder.endObject()

      bulkRequest.add(client.prepareUpdate(elastic_client.index_name, elastic_client.term_type_name, term.term)
        .setUpsert(builder))
    })

    val bulkResponse: BulkResponse = bulkRequest.get()

    val list_of_doc_res: List[UpdateDocumentResult] = bulkResponse.getItems.map(x => {
      UpdateDocumentResult(x.getIndex, x.getType, x.getId,
        x.getVersion,
        x.status == RestStatus.CREATED)
    }).toList

    val result: UpdateDocumentListResult = UpdateDocumentListResult(list_of_doc_res)
    Option {
      result
    }
  }

  def delete(termGetRequest: TermIdsRequest) : Option[DeleteDocumentListResult] = {
    val client: TransportClient = elastic_client.get_client()
    val bulkRequest : BulkRequestBuilder = client.prepareBulk()

    termGetRequest.ids.foreach( id => {
      val delete_request: DeleteRequestBuilder = client.prepareDelete(elastic_client.index_name,
        elastic_client.term_type_name, id)
      bulkRequest.add(delete_request)
    })
    val bulkResponse: BulkResponse = bulkRequest.get()

    val list_of_doc_res: List[DeleteDocumentResult] = bulkResponse.getItems.map(x => {
      DeleteDocumentResult(x.getIndex, x.getType, x.getId,
        x.getVersion,
        x.status == RestStatus.CREATED)
    }).toList

    val result: DeleteDocumentListResult = DeleteDocumentListResult(list_of_doc_res)
    Option {
      result
    }
  }

  def search_term(term: Term) : Option[TermsResults] = {
    val client: TransportClient = elastic_client.get_client()

    val search_builder : SearchRequestBuilder = client.prepareSearch(elastic_client.index_name)
      .setTypes(elastic_client.term_type_name)
      .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)

    val bool_query_builder : BoolQueryBuilder = QueryBuilders.boolQuery()
    bool_query_builder.must(QueryBuilders.termQuery("term.base", term.term))

    if (term.frequency.isDefined)
      bool_query_builder.must(QueryBuilders.termQuery("frequency", term.frequency.get))

    if (term.vector.isDefined) {
      //TODO: to complete
      //bool_query_builder.must(QueryBuilders.termQuery("verified", documentSearch.verified.get))
    }

    if (term.synonyms.isDefined)
      bool_query_builder.must(QueryBuilders.termQuery("synonyms", term.synonyms.get))

    if (term.antonyms.isDefined)
      bool_query_builder.must(QueryBuilders.termQuery("antonyms", term.antonyms.get))

    search_builder.setQuery(bool_query_builder)

    val search_response : SearchResponse = search_builder
      .execute()
      .actionGet()

    val documents : List[Term] = search_response.getHits.getHits.toList.map({ case(e) =>
      val item: SearchHit = e

      val source : Map[String, Any] = item.getSource.asScala.toMap

      val term : String = source.get("term").asInstanceOf[String]

      val synonyms: Option[String] = source.get("synonyms") match {
        case Some(t) => Option{t.asInstanceOf[String]}
        case None => None: Option[String]
      }

      val antonyms : Option[String] = source.get("antonyms") match {
        case Some(t) => Option {t.asInstanceOf[String]}
        case None => None: Option[String]
      }

      val frequency : Option[Double] = source.get("antonyms") match {
        case Some(t) => Option {t.asInstanceOf[Double]}
        case None => None: Option[Double]
      }

      val vector : Option[String] = source.get("antonyms") match {
        case Some(t) => Option {t.asInstanceOf[String]}
        case None => None: Option[String]
      }

      Term(term = term, //TODO: to complete
        synonyms = Option{Map.empty[String, Double]},
        antonyms = Option{Map.empty[String, Double]},
        frequency = frequency,
        vector = None: Option[Vector[Double]],
        score = Option{item.getScore.toDouble})
    })

    val terms: Terms = Terms(terms=documents)

    val max_score : Float = search_response.getHits.getMaxScore
    val total : Int = terms.terms.length
    val search_results : TermsResults = TermsResults(total = total, max_score = max_score,
      hits = terms)

    Option {
      search_results
    }
  }

  //given a text, return all terms which match
  def search(text: String) : Option[TermsResults] = {
    val client: TransportClient = elastic_client.get_client()

    val search_builder : SearchRequestBuilder = client.prepareSearch(elastic_client.index_name)
      .setTypes(elastic_client.term_type_name)
      .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)

    val bool_query_builder : BoolQueryBuilder = QueryBuilders.boolQuery()
    bool_query_builder.must(QueryBuilders.termQuery("term.base", text))
    search_builder.setQuery(bool_query_builder)

    val search_response : SearchResponse = search_builder
      .execute()
      .actionGet()

    val documents : List[Term] = search_response.getHits.getHits.toList.map({ case(e) =>
      val item: SearchHit = e

      val source : Map[String, Any] = item.getSource.asScala.toMap

      val term : String = source.get("term").asInstanceOf[String]

      val synonyms: Option[String] = source.get("synonyms") match {
        case Some(t) => Option{t.asInstanceOf[String]}
        case None => None: Option[String]
      }

      val antonyms : Option[String] = source.get("antonyms") match {
        case Some(t) => Option {t.asInstanceOf[String]}
        case None => None: Option[String]
      }

      val frequency : Option[Double] = source.get("antonyms") match {
        case Some(t) => Option {t.asInstanceOf[Double]}
        case None => None: Option[Double]
      }

      val vector : Option[String] = source.get("antonyms") match {
        case Some(t) => Option {t.asInstanceOf[String]}
        case None => None: Option[String]
      }

      Term(term = term, //TODO: to complete
        synonyms = Option{Map.empty[String, Double]},
        antonyms = Option{Map.empty[String, Double]},
        frequency = frequency,
        vector = None: Option[Vector[Double]],
        score = Option{item.getScore.toDouble})
    })

    val terms: Terms = Terms(terms=documents)

    val max_score : Float = search_response.getHits.getMaxScore
    val total : Int = terms.terms.length
    val search_results : TermsResults = TermsResults(total = total, max_score = max_score,
      hits = terms)

    Option {
      search_results
    }
  }

}




