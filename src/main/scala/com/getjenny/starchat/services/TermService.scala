package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 10/03/17.
  */

import akka.event.{Logging, LoggingAdapter}
import com.getjenny.starchat.SCActorSystem
import com.getjenny.starchat.entities._
import org.elasticsearch.action.admin.indices.analyze.{AnalyzeRequestBuilder, AnalyzeResponse}
import org.elasticsearch.action.bulk._
import org.elasticsearch.action.delete.DeleteRequestBuilder
import org.elasticsearch.action.get.{GetResponse, MultiGetItemResponse, MultiGetRequestBuilder, MultiGetResponse}
import org.elasticsearch.action.search.{SearchRequestBuilder, SearchResponse, SearchType}
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.xcontent.XContentBuilder
import org.elasticsearch.common.xcontent.XContentFactory._
import org.elasticsearch.index.query.{BoolQueryBuilder, QueryBuilder, QueryBuilders}
import org.elasticsearch.index.reindex.{BulkByScrollResponse, DeleteByQueryAction}
import org.elasticsearch.rest.RestStatus
import org.elasticsearch.search.SearchHit

import scala.collection.JavaConverters._
import scala.collection.immutable.{List, Map}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.Scalaz._

case class TermServiceException(message: String = "", cause: Throwable = None.orNull)
  extends Exception(message, cause)

/**
  * Implements functions, eventually used by TermResource
  */
object TermService {
  val elastiClient: TermClient.type = TermClient
  val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)

  def getIndexName(indexName: String, suffix: Option[String] = None): String = {
    indexName + "." + suffix.getOrElse(elastiClient.termIndexSuffix)
  }

  def payloadVectorToString[T](vector: Vector[T]): String = {
    vector.zipWithIndex.map(x => x._2.toString + "|" + x._1.toString).mkString(" ")
  }

  def payloadMapToString[T, U](payload: Map[T, U]): String = {
    payload.map(x => x._1.toString + "|" + x._2.toString).mkString(" ")
  }

  def payloadStringToDoubleVector(payload: String): Vector[Double] = {
    val vector: Vector[Double] = payload.split(" ").map(x => {
      val termTuple = x.split("\\|") match {
        case Array(_, value) => value.toDouble
        case _ =>
          throw TermServiceException("unable to convert payload string to double vector")
      }
      termTuple
    }).toVector
    vector
  }

  def payloadStringToMapStringDouble(payload: String): Map[String, Double] = {
    val m: Map[String, Double] = payload.split(" ").map(x => {
      val termTuple = x.split("\\|") match {
        case Array(key, value) => (key, value.toDouble)
        case _ =>
          throw TermServiceException("unable to convert string to string->double map")
      }
      termTuple
    }).toMap
    m
  }

  def payloadStringToMapIntDouble(payload: String): Map[Int, Double] = {
    val m: Map[Int, Double] = payload.split(" ").map(x => {
      val termTuple = x.split("\\|") match {
        case Array(index, value) => (index.toInt, value.toDouble)
        case _ =>
          throw TermServiceException("unable to convert string to int->double map")
      }
      termTuple
    }).toMap
    m
  }

  def payloadStringToMapStringString(payload: String): Map[String, String] = {
    val m: Map[String, String] = payload.split(" ").map(x => {
      val termTuple = x.split("\\|") match {
        case Array(key, value) => (key, value)
        case _ =>
          throw TermServiceException("unable to convert string to string->string map")
      }
      termTuple
    }).toMap
    m
  }

  def indexTerm(indexName: String, terms: Terms, refresh: Int) : Future[Option[IndexDocumentListResult]] = Future {
    val client: TransportClient = elastiClient.getClient()

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

      bulkRequest.add(client.prepareIndex().setIndex(getIndexName(indexName))
        .setType(elastiClient.termIndexSuffix)
        .setId(term.term)
        .setSource(builder))
    })

    val bulkResponse: BulkResponse = bulkRequest.get()

    val listOfDocRes: List[IndexDocumentResult] = bulkResponse.getItems.map(x => {
      IndexDocumentResult(x.getIndex, x.getType, x.getId,
      x.getVersion,
      x.status === RestStatus.CREATED)
    }).toList

    val result: IndexDocumentListResult = IndexDocumentListResult(listOfDocRes)
    Option {
      result
    }
  }

  def getTerm(indexName: String, terms_request: TermIdsRequest) : Option[Terms] = {
    val client: TransportClient = elastiClient.getClient()
    val multigetBuilder: MultiGetRequestBuilder = client.prepareMultiGet()
    multigetBuilder.add(getIndexName(indexName), elastiClient.termIndexSuffix, terms_request.ids:_*)
    val response: MultiGetResponse = multigetBuilder.get()
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

      val frequencyBase : Option[Double] = source.get("frequency_base") match {
        case Some(t) => Option {t.asInstanceOf[Double]}
        case None => None: Option[Double]
      }

      val frequencyStem : Option[Double] = source.get("frequency_stem") match {
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
        frequency_base = frequencyBase,
        frequency_stem = frequencyStem,
        vector = vector,
        score = None: Option[Double])
    })

    Option {Terms(terms=documents)}
  }

  def updateTerm(indexName: String, terms: Terms, refresh: Int) : Future[Option[UpdateDocumentListResult]] = Future {
    val client: TransportClient = elastiClient.getClient()

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

      bulkRequest.add(client.prepareUpdate().setIndex(getIndexName(indexName))
        .setType(elastiClient.termIndexSuffix)
        .setId(term.term)
        .setDoc(builder))
    })

    val bulkResponse: BulkResponse = bulkRequest.get()

    if (refresh =/= 0) {
      val refreshIndex = elastiClient.refreshIndex(getIndexName(indexName))
      if(refreshIndex.failed_shards_n > 0) {
        throw new Exception("KnowledgeBase : index refresh failed: (" + indexName + ")")
      }
    }

    val listOfDocRes: List[UpdateDocumentResult] = bulkResponse.getItems.map(x => {
      UpdateDocumentResult(x.getIndex, x.getType, x.getId,
        x.getVersion,
        x.status === RestStatus.CREATED)
    }).toList

    val result: UpdateDocumentListResult = UpdateDocumentListResult(listOfDocRes)
    Option {
      result
    }
  }

  def deleteAll(indexName: String): Future[Option[DeleteDocumentsResult]] = Future {
    val client: TransportClient = elastiClient.getClient()
    val qb: QueryBuilder = QueryBuilders.matchAllQuery()
    val response: BulkByScrollResponse =
      DeleteByQueryAction.INSTANCE.newRequestBuilder(client).setMaxRetries(10)
        .source(getIndexName(indexName))
        .filter(qb)
        .get()

    val deleted: Long = response.getDeleted

    val result: DeleteDocumentsResult = DeleteDocumentsResult(message = "delete", deleted = deleted)
    Option {result}
  }

  def delete(indexName: String, termGetRequest: TermIdsRequest, refresh: Int):
  Future[Option[DeleteDocumentListResult]] = Future {
    val client: TransportClient = elastiClient.getClient()
    val bulkRequest : BulkRequestBuilder = client.prepareBulk()

    termGetRequest.ids.foreach( id => {
      val deleteRequest: DeleteRequestBuilder = client.prepareDelete()
        .setIndex(getIndexName(indexName))
        .setType(elastiClient.termIndexSuffix)
        .setId(id)
      bulkRequest.add(deleteRequest)
    })
    val bulkResponse: BulkResponse = bulkRequest.get()

    if (refresh =/= 0) {
      val refreshIndex = elastiClient.refreshIndex(getIndexName(indexName))
      if(refreshIndex.failed_shards_n > 0) {
        throw new Exception("KnowledgeBase : index refresh failed: (" + indexName + ")")
      }
    }

    val listOfDocRes: List[DeleteDocumentResult] = bulkResponse.getItems.map(x => {
      DeleteDocumentResult(x.getIndex, x.getType, x.getId,
        x.getVersion,
        x.status =/= RestStatus.NOT_FOUND)
    }).toList

    val result: DeleteDocumentListResult = DeleteDocumentListResult(listOfDocRes)
    Option {
      result
    }
  }

  def searchTerm(indexName: String, term: Term) : Future[Option[TermsResults]] = Future {
    val client: TransportClient = elastiClient.getClient()

    val searchBuilder : SearchRequestBuilder = client.prepareSearch(getIndexName(indexName))
      .setTypes(elastiClient.termIndexSuffix)
      .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)

    val boolQueryBuilder : BoolQueryBuilder = QueryBuilders.boolQuery()
    boolQueryBuilder.must(QueryBuilders.termQuery("term.base", term.term))

    if (term.frequency_base.isDefined)
      boolQueryBuilder.must(QueryBuilders.termQuery("frequency_base", term.frequency_base.get))

    if (term.frequency_stem.isDefined)
      boolQueryBuilder.must(QueryBuilders.termQuery("frequency_stem", term.frequency_stem.get))

    if (term.synonyms.isDefined)
      boolQueryBuilder.must(QueryBuilders.termQuery("synonyms", term.synonyms.get))

    if (term.antonyms.isDefined)
      boolQueryBuilder.must(QueryBuilders.termQuery("antonyms", term.antonyms.get))

    if (term.tags.isDefined)
      boolQueryBuilder.must(QueryBuilders.termQuery("tags", term.tags.get))

    if (term.features.isDefined)
      boolQueryBuilder.must(QueryBuilders.termQuery("features", term.features.get))

    searchBuilder.setQuery(boolQueryBuilder)

    val searchResponse : SearchResponse = searchBuilder
      .execute()
      .actionGet()

    val documents : List[Term] = searchResponse.getHits.getHits.toList.map({ case(e) =>
      val item: SearchHit = e
      val source : Map[String, Any] = item.getSourceAsMap.asScala.toMap
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

      val frequencyBase : Option[Double] = source.get("frequency_base") match {
        case Some(t) => Option {t.asInstanceOf[Double]}
        case None => None: Option[Double]
      }

      val frequencyStem : Option[Double] = source.get("frequency_stem") match {
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
        frequency_base = frequencyBase,
        frequency_stem = frequencyStem,
        vector = vector,
        score = Option{item.getScore.toDouble})
    })

    val terms: Terms = Terms(terms=documents)

    val maxScore : Float = searchResponse.getHits.getMaxScore
    val total : Int = terms.terms.length
    val searchResults : TermsResults = TermsResults(total = total, max_score = maxScore,
      hits = terms)

    Option {
      searchResults
    }
  }

  //given a text, return all terms which match
  def search(indexName: String, text: String): Future[Option[TermsResults]] = Future {
    val client: TransportClient = elastiClient.getClient()

    val searchBuilder : SearchRequestBuilder = client.prepareSearch()
      .setIndices(getIndexName(indexName))
      .setTypes(elastiClient.termIndexSuffix)
      .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)

    val boolQueryBuilder : BoolQueryBuilder = QueryBuilders.boolQuery()
    boolQueryBuilder.should(QueryBuilders.matchQuery("term.base", text))
    searchBuilder.setQuery(boolQueryBuilder)

    val searchResponse : SearchResponse = searchBuilder
      .execute()
      .actionGet()

    val documents : List[Term] = searchResponse.getHits.getHits.toList.map({ case(e) =>
      val item: SearchHit = e
      val source : Map[String, Any] = item.getSourceAsMap.asScala.toMap
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

      val frequencyBase : Option[Double] = source.get("frequency_base") match {
        case Some(t) => Option {t.asInstanceOf[Double]}
        case None => None: Option[Double]
      }

      val frequencyStem : Option[Double] = source.get("frequency_stem") match {
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
        frequency_base = frequencyBase,
        frequency_stem = frequencyStem,
        vector = vector,
        score = Option{item.getScore.toDouble})
    })

    val terms: Terms = Terms(terms=documents)

    val maxScore : Float = searchResponse.getHits.getMaxScore
    val total : Int = terms.terms.length
    val searchResults : TermsResults = TermsResults(total = total, max_score = maxScore,
      hits = terms)

    Option {
      searchResults
    }
  }

  def esTokenizer(indexName: String, query: TokenizerQueryRequest) : Option[TokenizerResponse] = {
    val analyzer = query.tokenizer
    val isSupported: Boolean = TokenizersDescription.analyzers_map.isDefinedAt(analyzer)
    if(! isSupported) {
      throw new Exception("esTokenizer: analyzer not found or not supported: (" + analyzer + ")")
    }

    val client: TransportClient = elastiClient.getClient()

    val analyzerBuilder: AnalyzeRequestBuilder = client.admin.indices.prepareAnalyze(query.text)
    analyzerBuilder.setAnalyzer(analyzer)
    analyzerBuilder.setIndex(getIndexName(indexName))

    val analyzeResponse: AnalyzeResponse = analyzerBuilder
      .execute()
      .actionGet()

    val tokens : List[TokenizerResponseItem] =
      analyzeResponse.getTokens.listIterator.asScala.toList.map(t => {
        val responseItem: TokenizerResponseItem =
          TokenizerResponseItem(start_offset = t.getStartOffset,
            position = t.getPosition,
            end_offset = t.getEndOffset,
            token = t.getTerm,
            token_type = t.getType)
        responseItem
    })

    val response = Option { TokenizerResponse(tokens = tokens) }
    response
  }

  def textToVectors(indexName: String, text: String, analyzer: String = "stop", unique: Boolean = false):
  Option[TextTerms] = {
    val analyzerRequest = TokenizerQueryRequest(tokenizer = analyzer, text = text)
    val analyzersResponse = esTokenizer(indexName, analyzerRequest)

    val fullTokenList = analyzersResponse match {
      case Some(r) => r.tokens.map(e => e.token)
      case _  => List.empty[String]
    }

    val tokenList = if (unique) fullTokenList.toSet.toList else fullTokenList
    val returnValue = if(tokenList.nonEmpty) {
      val termsRequest = TermIdsRequest(ids = tokenList)
      val termList = getTerm(indexName, termsRequest)

      val textTerms = TextTerms(text = text,
        text_terms_n = tokenList.length,
        terms_found_n = termList.getOrElse(Terms(terms=List.empty[Term])).terms.length,
        terms = termList
      )
      Option { textTerms }
    } else {
      None: Option[TextTerms]
    }
    returnValue
  }

}
