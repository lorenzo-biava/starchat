package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 10/03/17.
  */

import java.io.{FileNotFoundException, InputStream}

import akka.event.{Logging, LoggingAdapter}
import com.getjenny.analyzer.util.VectorUtils
import com.getjenny.starchat.SCActorSystem
import com.getjenny.starchat.analyzer.utils.TextToVectorsTools
import com.getjenny.starchat.entities._
import com.getjenny.starchat.services.esclient.TermElasticClient
import com.getjenny.starchat.utils.Index
import org.elasticsearch.action.bulk._
import org.elasticsearch.action.admin.indices.analyze.{AnalyzeRequest, AnalyzeResponse}
import org.elasticsearch.action.delete.DeleteRequest
import org.elasticsearch.action.get.{GetResponse, MultiGetItemResponse, MultiGetRequest, MultiGetResponse}
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.search.{SearchRequest, SearchResponse, SearchType}
import org.elasticsearch.action.update.UpdateRequest
import org.elasticsearch.client.{RequestOptions, RestHighLevelClient}
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.common.xcontent.XContentBuilder
import org.elasticsearch.common.xcontent.XContentFactory._
import org.elasticsearch.index.query.{BoolQueryBuilder, QueryBuilders}
import org.elasticsearch.rest.RestStatus
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.builder.SearchSourceBuilder
import scalaz.Scalaz._

import scala.collection.JavaConverters._
import scala.collection.immutable.{List, Map}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source

case class TermServiceException(message: String = "", cause: Throwable = None.orNull)
  extends Exception(message, cause)

/**
  * Implements functions, eventually used by TermResource
  */
object TermService {
  private[this] val elasticClient: TermElasticClient.type = TermElasticClient
  private[this] val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)
  val defaultOrg = TermElasticClient.commonIndexDefaultOrgPattern

  /** fetch the index arbitrary pattern
    *
    * @return the index arbitrary pattern
    */
  def commonIndexArbitraryPattern: String = elasticClient.commonIndexArbitraryPattern

  /** transform a vector of numerical values to a string payload which can be stored on Elasticsearch
    *
    * @param vector a vector of values
    * @tparam T the type of the Vector
    * @return a string with the payload <value index>|<value>
    */
  private[this] def payloadVectorToString[T](vector: Vector[T]): String = {
    vector.zipWithIndex.map{case(term, index) => index.toString + "|" + term.toString}.mkString(" ")
  }

  /** transform a Key,Value map to a string payload which can be stored on Elasticsearch
    *
    * @param payload the map of values
    * @tparam T the type of the key
    * @tparam U the type of the value
    * @return a string with the payload <value index>|<value>
    */
  private[this] def payloadMapToString[T, U](payload: Map[T, U]): String = {
    payload.map{case(e1, e2) => e1.toString + "|" + e2.toString}.mkString(" ")
  }

  /** transform a payload string (non sparse vector) to a Double vector
    *
    * @param payload the payload <value index>|<value>
    * @return a double vector
    */
  private[this] def payloadStringToDoubleVector(payload: String): Vector[Double] = {
    val vector: Vector[Double] = payload.split(" ").map(x => {
      val termTuple: Double = x.split("\\|") match {
        case Array(_, value) => value.toDouble
        case _ =>
          throw TermServiceException("unable to convert payload string to double vector")
      }
      termTuple
    }).toVector
    vector
  }

  /** transform a payload string (non sparse vector) to a key, value Map[String, Double]
    *
    * @param payload the payload <value index>|<value>
    * @return a key, value map
    */
  private[this] def payloadStringToMapStringDouble(payload: String): Map[String, Double] = {
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

  /** transform a payload string (non sparse vector) to a key, double value Map[Int, Double]
    *
    * @param payload the payload <value index>|<value>
    * @return a key, value map
    */
  private[this] def payloadStringToMapIntDouble(payload: String): Map[Int, Double] = {
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

  /** transform a payload string (non sparse vector) to a key, value Map[String, String]
    *
    * @param payload the payload <value index>|<value>
    * @return a key, value map
    */
  private[this] def payloadStringToMapStringString(payload: String): Map[String, String] = {
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

  /** extract synonyms for a term
    *
    * @param lemma the lemma for which extract synonyms
    * @param synset the set of synonyms
    * @return the synonyms for the terms
    */
  private[this] def extractSyns(lemma: String, synset: Array[String]): Option[Map[String, Double]] = {
    val syns = synset.filter(_ =/= lemma).map(synLemma => (synLemma, 0.5d)).toMap
    if(syns.isEmpty) {
      Option.empty[Map[String, Double]]
    } else {
      Some(syns)
    }
  }

  /** Populate synonyms from resource file (a default synonyms list)
    *
    * @param indexName name of the index
    * @param groupSize a group size used to call a bulk indexing operation on ElasticSearch
    * @param refresh whether to call an index update on ElasticSearch or not
    * @return a return message with the number of successfully and failed indexing operations
    */
  def indexDefaultSynonyms(indexName: String, groupSize: Int = 2000,
                           refresh: Int = 0) : Future[Option[ReturnMessageData]] = Future {
    val (_, language, _) = Index.patternsFromIndex(indexName)
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

  /** index terms on Elasticsearch
    *
    * @param indexName the index name
    * @param terms the terms
    * @param refresh whether to call an index update on ElasticSearch or not
    * @return list of indexing responses
    */
  def indexTerm(indexName: String, terms: Terms, refresh: Int) : Future[Option[IndexDocumentListResult]] = Future {
    val client: RestHighLevelClient = elasticClient.client

    val bulkReq = new BulkRequest()

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

      val indexTermReq = new IndexRequest(Index.indexName(indexName, elasticClient.indexSuffix))
        .`type`(elasticClient.indexSuffix)
        .id(term.term)
        .source(builder)

      bulkReq.add(indexTermReq)
    })

    val bulkResponse: BulkResponse = client.bulk(bulkReq, RequestOptions.DEFAULT)

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

  /** fetch one or more terms from Elasticsearch
    *
    * @param indexName the index name
    * @param termsRequest the ids of the terms to be fetched
    * @return fetched terms
    */
  def termsById(indexName: String,
                termsRequest: TermIdsRequest) : Option[Terms] = {
    val documents: List[Term] = if(termsRequest.ids.nonEmpty) {
      val client: RestHighLevelClient = elasticClient.client

      val multiGetReq = new MultiGetRequest()

      termsRequest.ids.foreach{id =>
        multiGetReq.add(
          new MultiGetRequest.Item(Index.indexName(indexName, elasticClient.indexSuffix), elasticClient.indexSuffix, id)
        )
      }

      val response: MultiGetResponse = client.mget(multiGetReq, RequestOptions.DEFAULT)

      response.getResponses.toList
        .filter((p: MultiGetItemResponse) => p.getResponse.isExists).map({ case (e) =>
        val item: GetResponse = e.getResponse
        val source: Map[String, Any] = item.getSource.asScala.toMap

        val term: String = source.get("term") match {
          case Some(t) => t.asInstanceOf[String]
          case None => ""
        }

        val synonyms: Option[Map[String, Double]] = source.get("synonyms") match {
          case Some(t) =>
            val value: String = t.asInstanceOf[String]
            Option {
              payloadStringToMapStringDouble(value)
            }
          case None => None: Option[Map[String, Double]]
        }

        val antonyms: Option[Map[String, Double]] = source.get("antonyms") match {
          case Some(t) =>
            val value: String = t.asInstanceOf[String]
            Option {
              payloadStringToMapStringDouble(value)
            }
          case None => None: Option[Map[String, Double]]
        }

        val tags: Option[String] = source.get("tags") match {
          case Some(t) => Option {
            t.asInstanceOf[String]
          }
          case None => None: Option[String]
        }

        val features: Option[Map[String, String]] = source.get("features") match {
          case Some(t) =>
            val value: String = t.asInstanceOf[String]
            Option {
              payloadStringToMapStringString(value)
            }
          case None => None: Option[Map[String, String]]
        }

        val frequencyBase: Option[Double] = source.get("frequency_base") match {
          case Some(t) => Option {
            t.asInstanceOf[Double]
          }
          case None => None: Option[Double]
        }

        val frequencyStem: Option[Double] = source.get("frequency_stem") match {
          case Some(t) => Option {
            t.asInstanceOf[Double]
          }
          case None => None: Option[Double]
        }

        val vector: Option[Vector[Double]] = source.get("vector") match {
          case Some(t) =>
            val value: String = t.asInstanceOf[String]
            Option {
              payloadStringToDoubleVector(value)
            }
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
    } else {
      List.empty[Term]
    }

    Some(Terms(terms=documents))
  }

  /** fetch one or more terms from Elasticsearch
    *
    * @param indexName the index name
    * @param termsRequest the ids of the terms to be fetched
    * @return fetched terms
    */
  def getTermsByIdFuture(indexName: String,
                         termsRequest: TermIdsRequest) : Future[Option[Terms]] = Future {
    termsById(indexName, termsRequest)
  }

  /** update terms using a Future
    *
    * @param indexName index name
    * @param terms terms to update
    * @param refresh whether to call an index update on ElasticSearch or not
    * @return result of the update operations
    */
  def updateTermFuture(indexName: String, terms: Terms, refresh: Int) : Future[Option[UpdateDocumentListResult]] = Future {
    updateTerm(indexName, terms, refresh)
  }

  /** update terms, synchronous function
    *
    * @param indexName index name
    * @param terms terms to update
    * @param refresh whether to call an index update on ElasticSearch or not
    * @return result of the update operations
    */
  private[this] def updateTerm(indexName: String, terms: Terms, refresh: Int) : Option[UpdateDocumentListResult] = {
    val client: RestHighLevelClient = elasticClient.client

    val bulkReq : BulkRequest = new BulkRequest()

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

      val updateTermReq = new UpdateRequest()
        .index(Index.indexName(indexName, elasticClient.indexSuffix))
        .`type`(elasticClient.indexSuffix)
        .docAsUpsert(true)
        .id(term.term)
        .doc(builder)

      bulkReq.add(updateTermReq)
    })

    val bulkRes: BulkResponse = client.bulk(bulkReq, RequestOptions.DEFAULT)

    if (refresh =/= 0) {
      val refreshIndex = elasticClient.refresh(Index.indexName(indexName, elasticClient.indexSuffix))
      if(refreshIndex.failed_shards_n > 0) {
        throw TermServiceException("Term : index refresh failed: (" + indexName + ")")
      }
    }

    val listOfDocRes: List[UpdateDocumentResult] = bulkRes.getItems.map(x => {
      UpdateDocumentResult(x.getIndex, x.getType, x.getId,
        x.getVersion,
        x.status === RestStatus.CREATED)
    }).toList

    val result: UpdateDocumentListResult = UpdateDocumentListResult(listOfDocRes)
    Option {
      result
    }
  }

  /** delete all the terms in a table
    *
    * @param indexName index name
    * @return a DeleteDocumentsResult with the status of the delete operation
    */
  def deleteAll(indexName: String): Future[Option[DeleteDocumentsResult]] = Future {
    val client: RestHighLevelClient = elasticClient.client
    throw new Exception("Function to be implemented with version 7.0 of ES")
    /* TODO: to be implemented with version 7.0 of ES
    val qb: QueryBuilder = QueryBuilders.matchAllQuery()
    val response: BulkByScrollResponse =
      DeleteByQueryAction.INSTANCE.newRequestBuilder(client).setMaxRetries(10)
        .source(Index.indexName(indexName, elasticClient.indexSuffix))
        .filter(qb)
        .get()

    val deleted: Long = response.getDeleted

    val result: DeleteDocumentsResult = DeleteDocumentsResult(message = "delete", deleted = deleted)
    Option {result}
    */
  }

  /** delete one or more terms
    *
    * @param indexName index name
    * @param termGetRequest the list of term ids to delete
    * @param refresh whether to call an index update on ElasticSearch or not
    * @return DeleteDocumentListResult with the result of term delete operations
    */
  def delete(indexName: String, termGetRequest: TermIdsRequest, refresh: Int):
  Future[Option[DeleteDocumentListResult]] = Future {
    val client: RestHighLevelClient = elasticClient.client

    val bulkReq : BulkRequest = new BulkRequest()

    termGetRequest.ids.foreach( id => {
      val deleteReq = new DeleteRequest()
        .index(Index.indexName(indexName, elasticClient.indexSuffix))
        .`type`(elasticClient.indexSuffix)
        .id(id)
      bulkReq.add(deleteReq)
    })

    val bulkRes: BulkResponse = client.bulk(bulkReq, RequestOptions.DEFAULT)

    if (refresh =/= 0) {
      val refreshIndex = elasticClient.refresh(Index.indexName(indexName, elasticClient.indexSuffix))
      if(refreshIndex.failed_shards_n > 0) {
        throw TermServiceException("Term : index refresh failed: (" + indexName + ")")
      }
    }

    val listOfDocRes: List[DeleteDocumentResult] = bulkRes.getItems.map(x => {
      DeleteDocumentResult(x.getIndex, x.getType, x.getId,
        x.getVersion,
        x.status =/= RestStatus.NOT_FOUND)
    }).toList

    val result: DeleteDocumentListResult = DeleteDocumentListResult(listOfDocRes)
    Option {
      result
    }
  }

  /** search terms
    *
    * @param indexName index name
    * @param term search data structure
    * @return the retrieved terms
    */
  def searchTerm(indexName: String, term: SearchTerm) : Option[TermsResults] = {
    val client: RestHighLevelClient = elasticClient.client

    val analyzer = "space_punctuation"
    val analyzer_name = term.analyzer.getOrElse("space_punctuation")
    val term_field_name =
      TokenizersDescription.analyzers_map.get(analyzer_name) match {
        case Some(a) =>
          "term." + analyzer
        case _ =>
          throw TermServiceException("searchTerm: analyzer not found or not supported: (" +
            term.analyzer.getOrElse("") + ")")
      }

    val sourceReq: SearchSourceBuilder = new SearchSourceBuilder()
      .version(true)

    val searchReq = new SearchRequest(Index.indexName(indexName, elasticClient.indexSuffix))
      .source(sourceReq)
      .types(elasticClient.indexSuffix)
      .searchType(SearchType.DFS_QUERY_THEN_FETCH)

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

    sourceReq.query(boolQueryBuilder)

    val searchResponse : SearchResponse = client.search(searchReq, RequestOptions.DEFAULT)

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

  /** fetch two terms and calculate the distance between them
    *
    * @param indexName the index name
    * @param termsReq list of terms
    * @return the distance between terms
    */
  def termsDistance(indexName: String, termsReq: TermIdsRequest): Future[List[TermsDistanceRes]] = Future {
    val extractedTerms = termsById(indexName, TermIdsRequest(ids = termsReq.ids))
    val retrievedTerms = extractedTerms match {
      case Some(terms) =>
        terms.terms.map { case(t) => (t.term, t) }.toMap
      case _ =>
        Map.empty[String, Term]
    }

    val product = retrievedTerms.keys.flatMap(a => retrievedTerms.keys.map(b => (a, b))).filter(e => e._1 =/= e._2)
    product.map { case(t1, t2) =>
      val v1 = retrievedTerms(t1).vector.getOrElse(TextToVectorsTools.emptyVec())
      val v2 = retrievedTerms(t2).vector.getOrElse(TextToVectorsTools.emptyVec())
      TermsDistanceRes(
        term1 = t1,
        term2 = t2,
        vector1 = v1,
        vector2 = v2,
        cosDistance = VectorUtils.cosineDist(v1, v2),
        eucDistance = VectorUtils.euclideanDist(v1, v2)
      )
    }.toList
  }

  /** search terms on Elasticsearch
    *
    * @param indexName the index name
    * @param term term search data structure
    * @return fetched terms
    */
  def searchTermFuture(indexName: String,
                       term: SearchTerm): Future[Option[TermsResults]] = Future {
    searchTerm(indexName, term)
  }

  /** given a text, return all the matching terms
    *
    * @param indexName index term
    * @param text input text
    * @param analyzer the analyzer name to be used for text tokenization
    * @return the terms found
    */
  def search(indexName: String, text: String,
             analyzer: String = "space_punctuation"): Option[TermsResults] = {
    val client: RestHighLevelClient = elasticClient.client

    val term_field_name = TokenizersDescription.analyzers_map.get(analyzer) match {
      case Some(anlrz) => "term." + analyzer
      case _ =>
        throw TermServiceException("search: analyzer not found or not supported: (" + analyzer + ")")
    }

    val sourceReq: SearchSourceBuilder = new SearchSourceBuilder()
      .version(true)

    val searchReq = new SearchRequest(Index.indexName(indexName, elasticClient.indexSuffix))
      .source(sourceReq)
      .types(elasticClient.indexSuffix)
      .searchType(SearchType.DFS_QUERY_THEN_FETCH)

    val boolQueryBuilder : BoolQueryBuilder = QueryBuilders.boolQuery()
    boolQueryBuilder.should(QueryBuilders.matchQuery(term_field_name, text))
    sourceReq.query(boolQueryBuilder)

    val searchResponse : SearchResponse = client.search(searchReq, RequestOptions.DEFAULT)

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

  /** given a text, return all the matching terms
    *
    * @param indexName index term
    * @param text input text
    * @param analyzer the analyzer name to be used for text tokenization
    * @return fetched terms
    */
  def searchFuture(indexName: String,
                   text: String,
                   analyzer: String = "space_punctuation") : Future[Option[TermsResults]] = Future {
    val fetchedTerms = search(indexName, text, analyzer)
    fetchedTerms match {
      case Some(terms) => Some(terms)
      case _ => None
    }
  }

  /** tokenize a text
    *
    * @param indexName index name
    * @param query a TokenizerQueryRequest with the text to tokenize
    * @return a TokenizerResponse with the result of the tokenization
    */
  def esTokenizer(indexName: String, query: TokenizerQueryRequest) : Option[TokenizerResponse] = {
    val analyzer = TokenizersDescription.analyzers_map.get(query.tokenizer) match {
      case Some((analyzerEsName, _)) => analyzerEsName
      case _ =>
        throw TermServiceException("esTokenizer: analyzer not found or not supported: (" + query.tokenizer + ")")
    }

    val client: RestHighLevelClient = elasticClient.client

    val analyzerReq = new AnalyzeRequest()
      .index(Index.indexName(indexName, elasticClient.indexSuffix))
      .text(query.text)
      .analyzer(analyzer)

    val analyzeResponse: AnalyzeResponse = client.indices().analyze(analyzerReq, RequestOptions.DEFAULT)

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

  /** tokenize a sentence and extract term vectors for each token
    *
    * @param indexName index name
    * @param text input text
    * @param analyzer analyzer name
    * @param unique if true exclude from results duplicated terms
    * @return the TextTerms
    */
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
      val termList = termsById(indexName, termsRequest)

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

  /** fetch all documents and serve them through an iterator
    *
    * @param indexName index name
    * @param keepAlive the keep alive timeout for the ElasticSearch document scroller
    * @return an iterator for Items
    */
  def allDocuments(indexName: String, keepAlive: Long = 60000): Iterator[Term] = {
    val client: RestHighLevelClient = elasticClient.client

    val sourceReq: SearchSourceBuilder = new SearchSourceBuilder()
      .query(QueryBuilders.matchAllQuery)
      .size(100)

    val searchReq = new SearchRequest(Index.indexName(indexName, elasticClient.indexSuffix))
      .source(sourceReq)
      .types(elasticClient.indexSuffix)
      .scroll(new TimeValue(keepAlive))

    var scrollResp: SearchResponse = client.search(searchReq, RequestOptions.DEFAULT)

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

      scrollResp = client.search(searchReq, RequestOptions.DEFAULT)
      (documents, documents.nonEmpty)
    }.takeWhile{case (_, docNonEmpty) => docNonEmpty}
      .flatMap{case (doc, _) => doc}
    iterator
  }

}
