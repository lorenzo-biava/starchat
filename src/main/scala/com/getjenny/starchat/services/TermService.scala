package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 10/03/17.
  */

import java.io.{FileNotFoundException, InputStream}

import akka.event.{Logging, LoggingAdapter}
import com.getjenny.starchat.SCActorSystem
import com.getjenny.starchat.entities._
import org.elasticsearch.action.admin.indices.analyze.{AnalyzeRequestBuilder, AnalyzeResponse}
import org.elasticsearch.action.bulk._
import org.elasticsearch.action.delete.DeleteRequestBuilder
import org.elasticsearch.action.get.{GetResponse, MultiGetItemResponse, MultiGetRequestBuilder, MultiGetResponse}
import org.elasticsearch.action.search.{SearchRequestBuilder, SearchResponse, SearchType}
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.unit.TimeValue
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
import scala.io.Source
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
    vector.zipWithIndex.map{case(term, index) => index.toString + "|" + term.toString}.mkString(" ")
  }

  def payloadMapToString[T, U](payload: Map[T, U]): String = {
    payload.map{case(e1, e2) => e1.toString + "|" + e2.toString}.mkString(" ")
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

  private[this] def extractSyns(lemma:String, synset: Array[String]): Option[Map[String, Double]] = {
    val syns = synset.filter(_ =/= lemma).map(synLemma => (synLemma, 0.5d)).toMap
    if(syns.isEmpty) {
      Option.empty[Map[String, Double]]
    } else {
      Some(syns)
    }
  }

  def indexDefaultSynonyms(indexName: String, groupSize: Int = 2000, refresh: Int = 0) : Future[Option[ReturnMessageData]] = Future {
    // extract language from index name
    val indexLanguageRegex = "^(?:(index)_([a-z]{1,256})_([A-Za-z0-9_]{1,256}))$".r

    val (_, language, _) = indexName match {
      case indexLanguageRegex(indexPattern, languagePattern, arbitraryPattern) =>
        (indexPattern, languagePattern, arbitraryPattern)
      case _ => throw new Exception("index name is not well formed")
    }

    val synonymsPath: String = "/index_management/json_index_spec/" + language + "/synonyms.txt"
    val synonymsIs: Option[InputStream] = Some(getClass.getResourceAsStream(synonymsPath))
    val analyzerSource: Source = synonymsIs match {
      case Some(stream) => Source.fromInputStream(stream, "utf-8")
      case _ =>
        val message = "Check the file: (" + synonymsPath + ")"
        throw new FileNotFoundException(message)
    }

    val results = analyzerSource.getLines.grouped(groupSize).map(group => {
      val termList = group.par.map(entry => {
        val (typeAndLemma, synSet) = entry.split(",").splitAt(2)
        val (category, lemma) = (typeAndLemma.headOption.getOrElse(""), typeAndLemma.lastOption.getOrElse(""))
        category match {
          case "SYN" =>
            extractSyns(lemma, synSet) match {
              case Some(syns) =>
                Some(Term(term = lemma, synonyms = Some(syns)))
              case _ => Option.empty[Term]
            }
          case "ANT" =>
            extractSyns(lemma, synSet) match {
              case Some(syns) =>
                Some(Term(term = lemma, antonyms = Some(syns)))
              case _ => Option.empty[Term]
            }
          case _ => Option.empty[Term]
        }
      }).filter(_.nonEmpty).map(term => term.get).toList
      Terms(terms = termList)
    }).filter(_.terms.nonEmpty).map(terms => {
      updateTerm(indexName = indexName, terms = terms, refresh = refresh) match {
        case Some(updateDocumentListResult) =>
          log.info(s"Successfully indexed block of $groupSize synonyms : " + updateDocumentListResult.data.length)
          true
        case _ =>
          log.error(s"Failed indexed block of $groupSize synonyms")
          false
      }
    })
    val success = results.count(_ === true)
    val failure = results.count(_ === false)
    Some(ReturnMessageData(code=100, message = s"Indexed synonyms," +
      s" blocks of $groupSize items => success($success) failures($failure)"))
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

      val indexTermReq = client.prepareIndex().setIndex(getIndexName(indexName))
        .setType(elastiClient.termIndexSuffix)
        .setId(term.term)
        .setSource(builder)

      bulkRequest.add(indexTermReq)
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

  def getTermsById(indexName: String, terms_request: TermIdsRequest) : Option[Terms] = {
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

  def updateTermFuture(indexName: String, terms: Terms, refresh: Int) : Future[Option[UpdateDocumentListResult]] = Future {
    updateTerm(indexName, terms, refresh)
  }

  def updateTerm(indexName: String, terms: Terms, refresh: Int) : Option[UpdateDocumentListResult] = {
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
        .setDocAsUpsert(true)
        .setId(term.term)
        .setDoc(builder))
    })

    val bulkResponse: BulkResponse = bulkRequest.get()

    if (refresh =/= 0) {
      val refreshIndex = elastiClient.refreshIndex(getIndexName(indexName))
      if(refreshIndex.failed_shards_n > 0) {
        throw TermServiceException("Term : index refresh failed: (" + indexName + ")")
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
        throw TermServiceException("Term : index refresh failed: (" + indexName + ")")
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

  def searchTerm(indexName: String, term: SearchTerm) : Future[Option[TermsResults]] = Future {
    val client: TransportClient = elastiClient.getClient()

    val analyzer_name = term.analyzer.getOrElse("space_punctuation")
    val term_field_name =
      TokenizersDescription.analyzers_map.get(analyzer_name) match {
        case Some(a) => "term." + a
        case _ =>
          throw TermServiceException("searchTerm: analyzer not found or not supported: (" +
            term.analyzer.getOrElse("") + ")")
      }

    val searchBuilder : SearchRequestBuilder = client.prepareSearch(getIndexName(indexName))
      .setTypes(elastiClient.termIndexSuffix)
      .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)

    val boolQueryBuilder : BoolQueryBuilder = QueryBuilders.boolQuery()

    term.term match {
      case Some(term_property) =>
        boolQueryBuilder.must(QueryBuilders.termQuery(term_field_name, term_property))
      case _ => ;
    }

    term.frequency_base match {
      case Some(term_property) =>
        boolQueryBuilder.must(QueryBuilders.termQuery("frequency_base", term_property))
      case _ => ;
    }

    term.frequency_stem match {
      case Some(term_property) =>
        boolQueryBuilder.must(QueryBuilders.termQuery("frequency_stem", term_property))
      case _ => ;
    }

    term.synonyms match {
      case Some(term_property) =>
        boolQueryBuilder.must(QueryBuilders.termQuery("synonyms", term_property))
      case _ => ;
    }

    term.antonyms match {
      case Some(term_property) =>
        boolQueryBuilder.must(QueryBuilders.termQuery("antonyms", term_property))
      case _ => ;
    }

    term.tags match {
      case Some(term_property) =>
        boolQueryBuilder.must(QueryBuilders.termQuery("tags", term_property))
      case _ => ;
    }

    term.features match {
      case Some(term_property) =>
        boolQueryBuilder.must(QueryBuilders.termQuery("features", term_property))
      case _ => ;
    }

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
    Some(
      TermsResults(total = terms.terms.length, max_score = maxScore, hits = terms)
    )
  }

  //given a text, return all the matching terms
  def search(indexName: String, text: String,
             analyzer: String = "space_punctuation"): Future[Option[TermsResults]] = Future {
    val client: TransportClient = elastiClient.getClient()

    val term_field_name = TokenizersDescription.analyzers_map.get(analyzer) match {
      case Some(a) => "term." + analyzer
      case _ =>
        throw TermServiceException("search: analyzer not found or not supported: (" + analyzer + ")")
    }

    val searchBuilder : SearchRequestBuilder = client.prepareSearch()
      .setIndices(getIndexName(indexName))
      .setTypes(elastiClient.termIndexSuffix)
      .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)

    val boolQueryBuilder : BoolQueryBuilder = QueryBuilders.boolQuery()
    boolQueryBuilder.should(QueryBuilders.matchQuery(term_field_name, text))
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
    Some(
      TermsResults(total = terms.terms.length, max_score = maxScore, hits = terms)
    )
  }

  def esTokenizer(indexName: String, query: TokenizerQueryRequest) : Option[TokenizerResponse] = {
    val analyzer = TokenizersDescription.analyzers_map.get(query.tokenizer) match {
      case Some((analyzerEsName, _)) => analyzerEsName
      case _ =>
        throw TermServiceException("esTokenizer: analyzer not found or not supported: (" + query.tokenizer + ")")
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

  def textToVectors(indexName: String, text: String, analyzer: String = "stop",
                    unique: Boolean = false): Option[TextTerms] = {
    val analyzerRequest =
      TokenizerQueryRequest(tokenizer = analyzer, text = text) // analyzer is checked by esTokenizer
    val analyzersResponse = esTokenizer(indexName, analyzerRequest)

    val fullTokenList = analyzersResponse match {
      case Some(r) => r.tokens.map(e => e.token)
      case _  => List.empty[String]
    }

    val tokenList = if (unique) fullTokenList.toSet.toList else fullTokenList
    val returnValue = if(tokenList.nonEmpty) {
      val termsRequest = TermIdsRequest(ids = tokenList)
      val termList = getTermsById(indexName, termsRequest)

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

  def allDocuments(index_name: String, keepAlive: Long = 60000): Iterator[Term] = {
    val qb: QueryBuilder = QueryBuilders.matchAllQuery()
    val client: TransportClient = elastiClient.getClient()

    var scrollResp: SearchResponse = client
      .prepareSearch(getIndexName(index_name))
      .setScroll(new TimeValue(keepAlive))
      .setQuery(qb)
      .setSize(100).get()

    val iterator = Iterator.continually{
      val documents = scrollResp.getHits.getHits.toList.map( { case(e) =>
        val source : Map[String, Any] = e.getSourceAsMap.asScala.toMap

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

      scrollResp = client.prepareSearchScroll(scrollResp.getScrollId)
        .setScroll(new TimeValue(keepAlive)).execute().actionGet()
      (documents, documents.nonEmpty)
    }.takeWhile{case (_, docNonEmpty) => docNonEmpty}
      .flatMap{case (doc, _) => doc}
    iterator
  }

}
