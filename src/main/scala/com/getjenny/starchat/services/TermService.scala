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
import java.lang.String

import org.elasticsearch.action.admin.indices.analyze.AnalyzeRequestBuilder
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse.AnalyzeToken
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse


/**
  * Implements functions, eventually used by TermResource
  */
class TermService(implicit val executionContext: ExecutionContext) {
  val elastic_client = IndexManagementClient
  val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)

  def payloadVectorToString[T](vector: Vector[T]): String = {
    vector.zipWithIndex.map(x => x._2.toString + "|" + x._1.toString).mkString(" ")
  }

  def payloadMapToString[T, U](payload: Map[T, U]): String = {
    payload.map(x => x._1.toString + "|" + x._2.toString).mkString(" ")
  }

  def payloadStringToDoubleVector(payload: String): Vector[Double] = {
    val vector: Vector[Double] = payload.split(" ").map(x => {
      val term_tuple = x.split("\\|") match { case Array(t, r) => r.toDouble }
      term_tuple
    }).toVector
    vector
  }

  def payloadStringToMapStringDouble(payload: String): Map[String, Double] = {
    val m: Map[String, Double] = payload.split(" ").map(x => {
      val term_tuple = x.split("\\|") match { case Array(t, r) => (t, r.toDouble) }
      term_tuple
    }).toMap
    m
  }

  def payloadStringToMapIntDouble(payload: String): Map[Int, Double] = {
    val m: Map[Int, Double] = payload.split(" ").map(x => {
      val term_tuple = x.split("\\|") match { case Array(t, r) => (t.toInt, r.toDouble) }
      term_tuple
    }).toMap
    m
  }

  def payloadStringToMapStringString(payload: String): Map[String, String] = {
    val m: Map[String, String] = payload.split(" ").map(x => {
      val term_tuple = x.split("\\|") match { case Array(t, r) => (t, r) }
      term_tuple
    }).toMap
    m
  }

  def index_term(terms: Terms, refresh: Int) : Option[IndexDocumentListResult] = {
    val client: TransportClient = elastic_client.get_client()

    val bulkRequest : BulkRequestBuilder = client.prepareBulk()

    terms.terms.foreach( term => {
      val builder : XContentBuilder = jsonBuilder().startObject()
      builder.field("term", term.term)
      term.synonyms match {
        case Some(t) =>
          val indexable: String = payloadMapToString[String, Double](t)
          builder.field("synonyms", indexable)
        case None => ;
      }
      term.antonyms match {
        case Some(t) =>
          val indexable: String = payloadMapToString[String, Double](t)
          builder.field("antonyms", indexable)
        case None => ;
      }
      term.tags match {
        case Some(t) => builder.field("tags", t)
        case None => ;
      }
      term.features match {
        case Some(t) =>
          val indexable: String = payloadMapToString[String, String](t)
          builder.field("features", indexable)
        case None => ;
      }
      term.frequency_base match {
        case Some(t) => builder.field("frequency_base", t)
        case None => ;
      }
      term.frequency_stem match {
        case Some(t) => builder.field("frequency_stem", t)
        case None => ;
      }
      term.vector match {
        case Some(t) =>
          val indexable_vector: String = payloadVectorToString[Double](t)
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

      val term : String = source.get("term") match {
        case Some(t) => t.asInstanceOf[String]
        case None => ""
      }

      val synonyms : Option[Map[String, Double]] = source.get("synonyms") match {
        case Some(t) =>
          val value: String = t.asInstanceOf[String]
          Option{payloadStringToMapStringDouble(value)}
        case None => None: Option[Map[String, Double]]
      }

      val antonyms : Option[Map[String, Double]] = source.get("antonyms") match {
        case Some(t) =>
          val value: String = t.asInstanceOf[String]
          Option{payloadStringToMapStringDouble(value)}
        case None => None: Option[Map[String, Double]]
      }

      val tags : Option[String] = source.get("tags") match {
        case Some(t) => Option {t.asInstanceOf[String]}
        case None => None: Option[String]
      }

      val features : Option[Map[String, String]] = source.get("features") match {
        case Some(t) =>
          val value: String = t.asInstanceOf[String]
          Option{payloadStringToMapStringString(value)}
        case None => None: Option[Map[String, String]]
      }

      val frequency_base : Option[Double] = source.get("frequency_base") match {
        case Some(t) => Option {t.asInstanceOf[Double]}
        case None => None: Option[Double]
      }

      val frequency_stem : Option[Double] = source.get("frequency_stem") match {
        case Some(t) => Option {t.asInstanceOf[Double]}
        case None => None: Option[Double]
      }

      val vector : Option[Vector[Double]] = source.get("vector") match {
          case Some(t) =>
            val value: String = t.asInstanceOf[String]
            Option{payloadStringToDoubleVector(value)}
          case None => None: Option[Vector[Double]]
      }

      Term(term = term,
        synonyms = synonyms,
        antonyms = antonyms,
        tags = tags,
        features = features,
        frequency_base = frequency_base,
        frequency_stem = frequency_stem,
        vector = vector,
        score = None: Option[Double])
    })

    Option {Terms(terms=documents)}
  }

  def update_term(terms: Terms, refresh: Int) : Option[UpdateDocumentListResult] = {
    val client: TransportClient = elastic_client.get_client()

    val bulkRequest : BulkRequestBuilder = client.prepareBulk()

    terms.terms.foreach( term => {
      val builder : XContentBuilder = jsonBuilder().startObject()
      builder.field("term", term.term)
      term.synonyms match {
        case Some(t) =>
          val indexable: String = payloadMapToString[String, Double](t)
          builder.field("synonyms", indexable)
        case None => ;
      }
      term.antonyms match {
        case Some(t) =>
          val indexable: String = payloadMapToString[String, Double](t)
          builder.field("antonyms", indexable)
        case None => ;
      }
      term.tags match {
        case Some(t) => builder.field("tags", t)
        case None => ;
      }
      term.features match {
        case Some(t) =>
          val indexable: String = payloadMapToString[String, String](t)
          builder.field("features", indexable)
        case None => ;
      }
      term.frequency_base match {
        case Some(t) => builder.field("frequency_base", t)
        case None => ;
      }
      term.frequency_stem match {
        case Some(t) => builder.field("frequency_stem", t)
        case None => ;
      }
      term.vector match {
        case Some(t) =>
          val indexable_vector: String = payloadVectorToString[Double](t)
          builder.field("vector", indexable_vector)
        case None => ;
      }
      builder.endObject()

      bulkRequest.add(client.prepareUpdate(elastic_client.index_name, elastic_client.term_type_name, term.term)
          .setDoc(builder))
    })

    val bulkResponse: BulkResponse = bulkRequest.get()

    if (refresh != 0) {
      val refresh_index = elastic_client.refresh_index()
      if(refresh_index.failed_shards_n > 0) {
        throw new Exception("KnowledgeBase : index refresh failed: (" + elastic_client.index_name + ")")
      }
    }

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

  def delete(termGetRequest: TermIdsRequest, refresh: Int) : Option[DeleteDocumentListResult] = {
    val client: TransportClient = elastic_client.get_client()
    val bulkRequest : BulkRequestBuilder = client.prepareBulk()

    termGetRequest.ids.foreach( id => {
      val delete_request: DeleteRequestBuilder = client.prepareDelete(elastic_client.index_name,
        elastic_client.term_type_name, id)
      bulkRequest.add(delete_request)
    })
    val bulkResponse: BulkResponse = bulkRequest.get()

    if (refresh != 0) {
      val refresh_index = elastic_client.refresh_index()
      if(refresh_index.failed_shards_n > 0) {
        throw new Exception("KnowledgeBase : index refresh failed: (" + elastic_client.index_name + ")")
      }
    }

    val list_of_doc_res: List[DeleteDocumentResult] = bulkResponse.getItems.map(x => {
      DeleteDocumentResult(x.getIndex, x.getType, x.getId,
        x.getVersion,
        x.status != RestStatus.NOT_FOUND)
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

    if (term.frequency_base.isDefined)
      bool_query_builder.must(QueryBuilders.termQuery("frequency_base", term.frequency_base.get))

    if (term.frequency_stem.isDefined)
      bool_query_builder.must(QueryBuilders.termQuery("frequency_stem", term.frequency_stem.get))

    if (term.synonyms.isDefined)
      bool_query_builder.must(QueryBuilders.termQuery("synonyms", term.synonyms.get))

    if (term.antonyms.isDefined)
      bool_query_builder.must(QueryBuilders.termQuery("antonyms", term.antonyms.get))

    if (term.tags.isDefined)
      bool_query_builder.must(QueryBuilders.termQuery("tags", term.tags.get))

    if (term.features.isDefined)
      bool_query_builder.must(QueryBuilders.termQuery("features", term.features.get))

    search_builder.setQuery(bool_query_builder)

    val search_response : SearchResponse = search_builder
      .execute()
      .actionGet()

    val documents : List[Term] = search_response.getHits.getHits.toList.map({ case(e) =>
      val item: SearchHit = e

      val source : Map[String, Any] = item.getSource.asScala.toMap

      val term : String = source.get("term") match {
        case Some(t) => t.asInstanceOf[String]
        case None => ""
      }

      val synonyms : Option[Map[String, Double]] = source.get("synonyms") match {
        case Some(t) =>
          val value: String = t.asInstanceOf[String]
          Option{payloadStringToMapStringDouble(value)}
        case None => None: Option[Map[String, Double]]
      }

      val antonyms : Option[Map[String, Double]] = source.get("antonyms") match {
        case Some(t) =>
          val value: String = t.asInstanceOf[String]
          Option{payloadStringToMapStringDouble(value)}
        case None => None: Option[Map[String, Double]]
      }

      val tags : Option[String] = source.get("tags") match {
        case Some(t) => Option {t.asInstanceOf[String]}
        case None => None: Option[String]
      }

      val features : Option[Map[String, String]] = source.get("features") match {
        case Some(t) =>
          val value: String = t.asInstanceOf[String]
          Option{payloadStringToMapStringString(value)}
        case None => None: Option[Map[String, String]]
      }

      val frequency_base : Option[Double] = source.get("frequency_base") match {
        case Some(t) => Option {t.asInstanceOf[Double]}
        case None => None: Option[Double]
      }

      val frequency_stem : Option[Double] = source.get("frequency_stem") match {
        case Some(t) => Option {t.asInstanceOf[Double]}
        case None => None: Option[Double]
      }

      val vector : Option[Vector[Double]] = source.get("vector") match {
        case Some(t) =>
          val value: String = t.asInstanceOf[String]
          Option{payloadStringToDoubleVector(value)}
        case None => None: Option[Vector[Double]]
      }

      Term(term = term,
        synonyms = synonyms,
        antonyms = antonyms,
        tags = tags,
        features = features,
        frequency_base = frequency_base,
        frequency_stem = frequency_stem,
        vector = vector,
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
    bool_query_builder.should(QueryBuilders.matchQuery("term.base", text))
    search_builder.setQuery(bool_query_builder)

    val search_response : SearchResponse = search_builder
      .execute()
      .actionGet()

    val documents : List[Term] = search_response.getHits.getHits.toList.map({ case(e) =>
      val item: SearchHit = e

      val source : Map[String, Any] = item.getSource.asScala.toMap

      val term : String = source.get("term") match {
        case Some(t) => t.asInstanceOf[String]
        case None => ""
      }

      val synonyms : Option[Map[String, Double]] = source.get("synonyms") match {
        case Some(t) =>
          val value: String = t.asInstanceOf[String]
          Option{payloadStringToMapStringDouble(value)}
        case None => None: Option[Map[String, Double]]
      }

      val antonyms : Option[Map[String, Double]] = source.get("antonyms") match {
        case Some(t) =>
          val value: String = t.asInstanceOf[String]
          Option{payloadStringToMapStringDouble(value)}
        case None => None: Option[Map[String, Double]]
      }

      val tags : Option[String] = source.get("tags") match {
        case Some(t) => Option {t.asInstanceOf[String]}
        case None => None: Option[String]
      }

      val features : Option[Map[String, String]] = source.get("features") match {
        case Some(t) =>
          val value: String = t.asInstanceOf[String]
          Option{payloadStringToMapStringString(value)}
        case None => None: Option[Map[String, String]]
      }

      val frequency_base : Option[Double] = source.get("frequency_base") match {
        case Some(t) => Option {t.asInstanceOf[Double]}
        case None => None: Option[Double]
      }

      val frequency_stem : Option[Double] = source.get("frequency_stem") match {
        case Some(t) => Option {t.asInstanceOf[Double]}
        case None => None: Option[Double]
      }

      val vector : Option[Vector[Double]] = source.get("vector") match {
        case Some(t) =>
          val value: String = t.asInstanceOf[String]
          Option{payloadStringToDoubleVector(value)}
        case None => None: Option[Vector[Double]]
      }

      Term(term = term,
        synonyms = synonyms,
        antonyms = antonyms,
        tags = tags,
        features = features,
        frequency_base = frequency_base,
        frequency_stem = frequency_stem,
        vector = vector,
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

  def esTokenizer(query: TokenizerQueryRequest) : Option[TokenizerResponse] = {
    val analyzer = query.tokenizer
    val is_supported: Boolean = TokenizersDescription.analyzers_map.isDefinedAt(analyzer)
    if(! is_supported) {
      throw new Exception("esTokenizer: analyzer not found or not supported: (" + analyzer + ")")
    }

    val client: TransportClient = elastic_client.get_client()

    val analyzer_builder: AnalyzeRequestBuilder = client.admin.indices.prepareAnalyze(query.text)
    analyzer_builder.setAnalyzer(analyzer)
    analyzer_builder.setIndex(elastic_client.index_name)

    val analyze_response: AnalyzeResponse = analyzer_builder
      .execute()
      .actionGet()

    val tokens : List[TokenizerResponseItem] =
      analyze_response.getTokens.listIterator.asScala.toList.map(t => {
        val response_item: TokenizerResponseItem =
          TokenizerResponseItem(start_offset = t.getStartOffset,
            position = t.getPosition,
            end_offset = t.getEndOffset,
            token = t.getTerm,
            token_type = t.getType)
        response_item
    })

    val response = Option { TokenizerResponse(tokens = tokens) }
    response
  }

  def textToVectors(text: String, analyzer: String = "stop", unique: Boolean = false):
  Option[TextTerms] = {
    val analyzer_request = TokenizerQueryRequest(tokenizer = analyzer, text = text)
    val analyzers_response = esTokenizer(analyzer_request)
    val full_token_list = analyzers_response match {
      case Some(r) => r.tokens.map(e => e.token)
      case _  => List.empty[String]
    }
    val token_list = if (unique) full_token_list.toSet.toList else full_token_list
    val return_value = if(token_list.nonEmpty) {
      val terms_request = TermIdsRequest(ids = token_list)
      val term_list = get_term(terms_request)
      val text_terms = TextTerms(text = text,
        text_terms_n = token_list.length,
        terms_found_n = term_list.getOrElse(Terms(terms=List.empty[Term])).terms.length,
        terms = term_list
      )
      Option { text_terms }
    } else {
      None: Option[TextTerms]
    }
    return_value
  }

}
