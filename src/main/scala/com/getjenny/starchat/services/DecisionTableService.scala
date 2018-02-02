package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 01/07/16.
  */

import java.io.File

import akka.event.{Logging, LoggingAdapter}
import com.getjenny.starchat.SCActorSystem
import com.getjenny.starchat.entities._
import org.apache.lucene.search.join._
import org.elasticsearch.action.delete.DeleteResponse
import org.elasticsearch.action.get.{GetResponse, MultiGetItemResponse, MultiGetRequestBuilder, MultiGetResponse}
import org.elasticsearch.action.search.{SearchRequestBuilder, SearchResponse, SearchType}
import org.elasticsearch.action.update.UpdateResponse
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.unit._
import org.elasticsearch.common.xcontent.XContentBuilder
import org.elasticsearch.common.xcontent.XContentFactory._
import org.elasticsearch.index.query.{BoolQueryBuilder, InnerHitBuilder, QueryBuilder, QueryBuilders}
import org.elasticsearch.index.reindex.{BulkByScrollResponse, DeleteByQueryAction}
import org.elasticsearch.rest.RestStatus
import org.elasticsearch.search.SearchHit

import scala.collection.JavaConverters._
import scala.collection.immutable.{List, Map}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}
import scalaz.Scalaz._

case class DecisionTableServiceException(message: String = "", cause: Throwable = None.orNull)
  extends Exception(message, cause)

/**
  * Implements functions, eventually used by DecisionTableResource, for searching, get next response etc
  */
object DecisionTableService {
  val elasticClient: DecisionTableElasticClient.type = DecisionTableElasticClient
  val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)

  val queriesScoreMode: Map[String, ScoreMode] =
    Map[String, ScoreMode]("min" -> ScoreMode.Min,
      "max" -> ScoreMode.Max, "avg" -> ScoreMode.Avg, "total" -> ScoreMode.Total)

  def getIndexName(indexName: String, suffix: Option[String] = None): String = {
    indexName + "." + suffix.getOrElse(elasticClient.dtIndexSuffix)
  }

  def search(indexName: String, documentSearch: DTDocumentSearch): Future[Option[SearchDTDocumentsResults]] = {
    val client: TransportClient = elasticClient.getClient()
    val searchBuilder : SearchRequestBuilder = client.prepareSearch(getIndexName(indexName))
      .setTypes(elasticClient.dtIndexSuffix)
      .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)

    val minScore = documentSearch.min_score.getOrElse(
      Option{elasticClient.queryMinThreshold}.getOrElse(0.0f)
    )

    val boostExactMatchFactor = documentSearch.boost_exact_match_factor.getOrElse(
      Option{elasticClient.boostExactMatchFactor}.getOrElse(1.0f)
    )

    searchBuilder.setMinScore(minScore)

    val boolQueryBuilder : BoolQueryBuilder = QueryBuilders.boolQuery()
    if (documentSearch.state.isDefined)
      boolQueryBuilder.must(QueryBuilders.termQuery("state", documentSearch.state.get))

    if (documentSearch.execution_order.isDefined)
      boolQueryBuilder.must(QueryBuilders.matchQuery("execution_order", documentSearch.state.get))

    if(documentSearch.queries.isDefined) {
      val nestedQuery: QueryBuilder = QueryBuilders.nestedQuery(
        "queries",
        QueryBuilders.boolQuery()
          .must(QueryBuilders.matchQuery("queries.query.stem_bm25", documentSearch.queries.get))
          .should(QueryBuilders.matchPhraseQuery("queries.query.raw", documentSearch.queries.get)
            .boost(1 + (minScore * boostExactMatchFactor))
          ),
        queriesScoreMode.getOrElse(elasticClient.queriesScoreMode, ScoreMode.Max)
      ).ignoreUnmapped(true).innerHit(new InnerHitBuilder().setSize(100))
      boolQueryBuilder.must(nestedQuery)
    }

    searchBuilder.setQuery(boolQueryBuilder)

    val searchResponse : SearchResponse = searchBuilder
      .setFrom(documentSearch.from.getOrElse(0)).setSize(documentSearch.size.getOrElse(10))
      .execute()
      .actionGet()

    val documents : Option[List[SearchDTDocument]] =
      Option { searchResponse.getHits.getHits.toList.map( { case(e) =>

        val item: SearchHit = e

        val state : String = item.getId

        val source : Map[String, Any] = item.getSourceAsMap.asScala.toMap

        val executionOrder: Int = source.get("execution_order") match {
          case Some(t) => t.asInstanceOf[Int]
          case None => 0
        }

        val maxStateCount : Int = source.get("max_state_count") match {
          case Some(t) => t.asInstanceOf[Int]
          case None => 0
        }

        val analyzer : String = source.get("analyzer") match {
          case Some(t) => t.asInstanceOf[String]
          case None => ""
        }

        val queries : List[String] = source.get("queries") match {
          case Some(t) =>
            val offsets = e.getInnerHits.get("queries").getHits.toList.map(innerHit => {
              innerHit.getNestedIdentity.getOffset
            })
            val query_array = t.asInstanceOf[java.util.ArrayList[java.util.HashMap[String, String]]].asScala.toList
              .map(q_e => q_e.get("query"))
            val queriesOrdered : List[String] = offsets.map(i => query_array(i))
            queriesOrdered
          case None => List.empty[String]
        }

        val bubble : String = source.get("bubble") match {
          case Some(t) => t.asInstanceOf[String]
          case None => ""
        }

        val action : String = source.get("action") match {
          case Some(t) => t.asInstanceOf[String]
          case None => ""
        }

        val actionInput : Map[String,String] = source.get("action_input") match {
          case Some(t) => t.asInstanceOf[java.util.HashMap[String,String]].asScala.toMap
          case None => Map[String, String]()
        }

        val stateData : Map[String,String] = source.get("state_data") match {
          case Some(t) => t.asInstanceOf[java.util.HashMap[String,String]].asScala.toMap
          case None => Map[String, String]()
        }

        val successValue : String = source.get("success_value") match {
          case Some(t) => t.asInstanceOf[String]
          case None => ""
        }

        val failureValue : String = source.get("failure_value") match {
          case Some(t) => t.asInstanceOf[String]
          case None => ""
        }

        val document : DTDocument = DTDocument(state = state, execution_order = executionOrder,
          max_state_count = maxStateCount,
          analyzer = analyzer, queries = queries, bubble = bubble,
          action = action, action_input = actionInput, state_data = stateData,
          success_value = successValue, failure_value = failureValue)

        val searchDocument : SearchDTDocument = SearchDTDocument(score = item.getScore, document = document)
        searchDocument
      }) }

    val filteredDoc : List[SearchDTDocument] = documents.getOrElse(List[SearchDTDocument]())

    val maxScore : Float = if(searchResponse.getHits.totalHits > 0) {
      searchResponse.getHits.getMaxScore
    } else {
      0.0f
    }

    val total : Int = filteredDoc.length
    val searchResults : SearchDTDocumentsResults = SearchDTDocumentsResults(total = total, max_score = maxScore,
      hits = filteredDoc)

    val searchResultsOption : Future[Option[SearchDTDocumentsResults]] = Future { Option { searchResults } }
    searchResultsOption
  }

  def searchDtQueries(indexName: String, userText: String): Option[SearchDTDocumentsResults] = {
    val dtDocumentSearch: DTDocumentSearch =
      DTDocumentSearch(from = Option {
        0
      }, size = Option {
        10000
      },
        min_score = Option {
          elasticClient.queryMinThreshold
        },
        execution_order = None: Option[Int],
        boost_exact_match_factor = Option {
          elasticClient.boostExactMatchFactor
        },
        state = None: Option[String], queries = Option {
          userText
        })

    val searchResult: Try[Option[SearchDTDocumentsResults]] =
      Await.ready(this.search(indexName, dtDocumentSearch), 10.seconds).value match {
        case Some(c) => c
        case _ => throw DecisionTableServiceException("Search resulted in an empty data structure")
      }
    val foundDocuments = searchResult match {
      case Success(t) =>
        t
      case Failure(e) =>
        val message = "ResponseService search"
        log.error(message + " : " + e.getMessage)
        throw DecisionTableServiceException(message, e)
    }
    foundDocuments
  }

  def resultsToMap(results: Option[SearchDTDocumentsResults]): Map[String, Any] = {
    results match {
      case Some(searchRes) =>
        val m: Map[String, (Float, SearchDTDocument)] = searchRes.hits.map(doc => {
          (doc.document.state, (doc.score, doc))
        }).toMap
        Map("dt_queries_search_result" -> Option{m})
      case _ => Map.empty[String, (Float, SearchDTDocument)]
    }
  }

  def create(indexName: String, document: DTDocument, refresh: Int): Future[Option[IndexDocumentResult]] = Future {
    val builder : XContentBuilder = jsonBuilder().startObject()

    builder.field("state", document.state)
    builder.field("execution_order", document.execution_order)
    builder.field("max_state_count", document.max_state_count)
    builder.field("analyzer", document.analyzer)

    val array = builder.startArray("queries")
    document.queries.foreach(q => {
      array.startObject().field("query", q).endObject()
    })
    array.endArray()

    builder.field("bubble", document.bubble)
    builder.field("action", document.action)

    val actionInputBuilder : XContentBuilder = builder.startObject("action_input")
    for ((k,v) <- document.action_input) actionInputBuilder.field(k,v)
    actionInputBuilder.endObject()

    val stateDataBuilder : XContentBuilder = builder.startObject("state_data")
    for ((k,v) <- document.state_data) stateDataBuilder.field(k,v)
    stateDataBuilder.endObject()

    builder.field("success_value", document.success_value)
    builder.field("failure_value", document.failure_value)

    builder.endObject()

    val client: TransportClient = elasticClient.getClient()
    val response = client.prepareIndex().setIndex(getIndexName(indexName))
      .setType(elasticClient.dtIndexSuffix)
      .setId(document.state)
      .setSource(builder).get()

    if (refresh =/= 0) {
      val refreshIndex = elasticClient.refreshIndex(getIndexName(indexName))
      if(refreshIndex.failed_shards_n > 0) {
        throw new Exception("DecisionTable : index refresh failed: (" + indexName + ")")
      }
    }

    val docResult: IndexDocumentResult = IndexDocumentResult(index = response.getIndex,
      dtype = response.getType,
      id = response.getId,
      version = response.getVersion,
      created = response.status === RestStatus.CREATED
    )

    Option {docResult}
  }

  def update(indexName: String, id: String, document: DTDocumentUpdate, refresh: Int):
  Future[Option[UpdateDocumentResult]] = Future {
    val builder : XContentBuilder = jsonBuilder().startObject()

    document.analyzer match {
      case Some(t) => builder.field("analyzer", t)
      case None => ;
    }

    document.execution_order match {
      case Some(t) => builder.field("execution_order", t)
      case None => ;
    }
    document.max_state_count match {
      case Some(t) => builder.field("max_state_count", t)
      case None => ;
    }
    document.queries match {
      case Some(t) =>

        val array = builder.startArray("queries")
        t.foreach(q => {
          array.startObject().field("query", q).endObject()
        })
        array.endArray()
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
        val actionInputBuilder : XContentBuilder = builder.startObject("action_input")
        for ((k,v) <- t) actionInputBuilder.field(k,v)
        actionInputBuilder.endObject()
      case None => ;
    }
    document.state_data match {
      case Some(t) =>
        val stateDataBuilder : XContentBuilder = builder.startObject("state_data")
        for ((k,v) <- t) stateDataBuilder.field(k,v)
        stateDataBuilder.endObject()
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

    val client: TransportClient = elasticClient.getClient()
    val response: UpdateResponse = client.prepareUpdate().setIndex(getIndexName(indexName))
      .setType(elasticClient.dtIndexSuffix).setId(id)
      .setDoc(builder)
      .get()

    if (refresh =/= 0) {
      val refreshIndex = elasticClient.refreshIndex(getIndexName(indexName))
      if(refreshIndex.failed_shards_n > 0) {
        throw new Exception("DecisionTable : index refresh failed: (" + indexName + ")")
      }
    }

    val docResult: UpdateDocumentResult = UpdateDocumentResult(index = response.getIndex,
      dtype = response.getType,
      id = response.getId,
      version = response.getVersion,
      created = response.status === RestStatus.CREATED
    )

    Option {docResult}
  }

  def deleteAll(indexName: String): Future[Option[DeleteDocumentsResult]] = Future {
    val client: TransportClient = elasticClient.getClient()
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

  def delete(indexName: String, id: String, refresh: Int): Future[Option[DeleteDocumentResult]] = Future {
    val client: TransportClient = elasticClient.getClient()
    val response: DeleteResponse = client.prepareDelete().setIndex(getIndexName(indexName))
      .setType(elasticClient.dtIndexSuffix).setId(id).get()

    if (refresh =/= 0) {
      val refreshIndex = elasticClient.refreshIndex(getIndexName(indexName))
      if(refreshIndex.failed_shards_n > 0) {
        throw new Exception("DecisionTable : index refresh failed: (" + indexName + ")")
      }
    }

    val docResult: DeleteDocumentResult = DeleteDocumentResult(index = response.getIndex,
      dtype = response.getType,
      id = response.getId,
      version = response.getVersion,
      found = response.status =/= RestStatus.NOT_FOUND
    )

    Option {docResult}
  }

  def getDTDocuments(index_name: String): Future[Option[SearchDTDocumentsResults]] = {
    val client: TransportClient = elasticClient.getClient()

    val qb : QueryBuilder = QueryBuilders.matchAllQuery()
    val scroll_resp : SearchResponse = client.prepareSearch(getIndexName(index_name))
      .setTypes(elasticClient.dtIndexSuffix)
      .setQuery(qb)
      .setScroll(new TimeValue(60000))
      .setSize(10000).get()

    //get a map of stateId -> AnalyzerItem (only if there is smt in the field "analyzer")
    val decisiontableContent : List[SearchDTDocument] = scroll_resp.getHits.getHits.toList.map({ e =>
      val item: SearchHit = e
      val state : String = item.getId
      val source : Map[String, Any] = item.getSourceAsMap.asScala.toMap

      val executionOrder : Int = source.get("execution_order") match {
        case Some(t) => t.asInstanceOf[Int]
        case None => 0
      }

      val maxStateCount : Int = source.get("max_state_count") match {
        case Some(t) => t.asInstanceOf[Int]
        case None => 0
      }

      val analyzer : String = source.get("analyzer") match {
        case Some(t) => t.asInstanceOf[String]
        case None => ""
      }

      val queries : List[String] = source.get("queries") match {
        case Some(t) => t.asInstanceOf[java.util.ArrayList[java.util.HashMap[String, String]]]
          .asScala.map(query =>
          query.asScala.get("query")).filter(_.isDefined).toList.map(_.get)
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

      val actionInput : Map[String,String] = source.get("action_input") match {
        case Some(t) => t.asInstanceOf[java.util.HashMap[String,String]].asScala.toMap
        case None => Map[String,String]()
      }

      val stateData : Map[String,String] = source.get("state_data") match {
        case Some(t) => t.asInstanceOf[java.util.HashMap[String,String]].asScala.toMap
        case None => Map[String,String]()
      }

      val successValue : String = source.get("success_value") match {
        case Some(t) => t.asInstanceOf[String]
        case None => ""
      }

      val failureValue : String = source.get("failure_value") match {
        case Some(t) => t.asInstanceOf[String]
        case None => ""
      }

      val document : DTDocument = DTDocument(state = state, execution_order = executionOrder,
        max_state_count = maxStateCount,
        analyzer = analyzer, queries = queries, bubble = bubble,
        action = action, action_input = actionInput, state_data = stateData,
        success_value = successValue, failure_value = failureValue)

      val searchDocument : SearchDTDocument = SearchDTDocument(score = .0f, document = document)
      searchDocument
    }).sortBy(_.document.state)

    val maxScore : Float = .0f
    val total : Int = decisiontableContent.length
    val searchResults : SearchDTDocumentsResults = SearchDTDocumentsResults(total = total, max_score = maxScore,
      hits = decisiontableContent)

    Future{Option{searchResults}}
  }

  def read(indexName: String, ids: List[String]): Future[Option[SearchDTDocumentsResults]] = {
    val client: TransportClient = elasticClient.getClient()
    val multigetBuilder: MultiGetRequestBuilder = client.prepareMultiGet()

    if(ids.nonEmpty) {
      // a list of specific ids was requested
      multigetBuilder.add(getIndexName(indexName), elasticClient.dtIndexSuffix, ids: _*)
      val response: MultiGetResponse = multigetBuilder.get()

      val documents: Option[List[SearchDTDocument]] = Option {
        response.getResponses
          .toList.filter((p: MultiGetItemResponse) => p.getResponse.isExists).map({ case (e) =>

          val item: GetResponse = e.getResponse

          val state: String = item.getId

          val source: Map[String, Any] = item.getSource.asScala.toMap

          val executionOrder: Int = source.get("execution_order") match {
            case Some(t) => t.asInstanceOf[Int]
            case None => 0
          }

          val maxStateCount: Int = source.get("max_state_count") match {
            case Some(t) => t.asInstanceOf[Int]
            case None => 0
          }

          val analyzer: String = source.get("analyzer") match {
            case Some(t) => t.asInstanceOf[String]
            case None => ""
          }

          val queries: List[String] = source.get("queries") match {
            case Some(t) => t.asInstanceOf[java.util.ArrayList[java.util.HashMap[String, String]]]
              .asScala.map { res => Some(res.getOrDefault("query", None.orNull)) }
              .filter(_.isDefined).map(_.get).toList
            case None => List[String]()
          }

          val bubble: String = source.get("bubble") match {
            case Some(t) => t.asInstanceOf[String]
            case None => ""
          }

          val action: String = source.get("action") match {
            case Some(t) => t.asInstanceOf[String]
            case None => ""
          }

          val actionInput: Map[String, String] = source.get("action_input") match {
            case Some(t) => t.asInstanceOf[java.util.HashMap[String, String]].asScala.toMap
            case None => Map[String, String]()
          }

          val stateData: Map[String, String] = source.get("state_data") match {
            case Some(t) => t.asInstanceOf[java.util.HashMap[String, String]].asScala.toMap
            case None => Map[String, String]()
          }

          val successValue: String = source.get("success_value") match {
            case Some(t) => t.asInstanceOf[String]
            case None => ""
          }

          val failureValue: String = source.get("failure_value") match {
            case Some(t) => t.asInstanceOf[String]
            case None => ""
          }

          val document: DTDocument = DTDocument(state = state, execution_order = executionOrder,
            max_state_count = maxStateCount,
            analyzer = analyzer, queries = queries, bubble = bubble,
            action = action, action_input = actionInput, state_data = stateData,
            success_value = successValue, failure_value = failureValue)

          val searchDocument: SearchDTDocument = SearchDTDocument(score = .0f, document = document)
          searchDocument
        })
      }

      val filteredDoc: List[SearchDTDocument] = documents.getOrElse(List[SearchDTDocument]())

      val maxScore: Float = .0f
      val total: Int = filteredDoc.length
      val searchResults: SearchDTDocumentsResults = SearchDTDocumentsResults(total = total, max_score = maxScore,
        hits = filteredDoc)

      Future {
        Option {
          searchResults
        }
      }
    } else {
      // fetching all documents
      getDTDocuments(indexName)
    }
  }


  def indexCSVFileIntoDecisionTable(indexName: String, file: File, skiplines: Int = 1, separator: Char = ','):
  Future[Option[IndexDocumentListResult]] = {
    val documents: Try[Option[List[DTDocument]]] =
      Await.ready(
        Future{
          Option{
            FileToDTDocuments.getDTDocumentsFromCSV(log = log, file = file, skiplines = skiplines, separator = separator)
          }
        }, 30.seconds).value
        .getOrElse(Failure(throw new Exception("indexCSVFileIntoDecisionTable: empty list of documents from csv")))

    val documentList = documents match {
      case Success(t) =>
        t
      case Failure(e) =>
        val message = "error indexing CSV file, check syntax"
        log.error(message + " : " + e.getMessage)
        throw new Exception(message, e)
    }

    val indexDocumentListResult = documentList match {
      case Some(t) =>
        val values = t.map(dtDocument => {
          val indexingResult: Try[Option[IndexDocumentResult]] =
            Await.ready(create(indexName, dtDocument, 1), 10.seconds).value.getOrElse(
              Failure(throw new Exception("indexCSVFileIntoDecisionTable: operation result was empty"))
            )
          indexingResult match {
            case Success(result) =>
              result match {
                case Some(indexingDocuments) => indexingDocuments
                case _ =>
                  throw new Exception("indexCSVFileIntoDecisionTable: indexingDocuments was empty")
              }
            case Failure(e) =>
              val message = "Cannot index document: " + dtDocument.state
              log.error(message + " : " + e.getMessage)
              throw new Exception(message, e)
          }
        })
        Option { IndexDocumentListResult(data = values) }
      case _ =>
        val message = "I could not index any document"
        log.error(message)
        throw new Exception(message)
    }

    Future { indexDocumentListResult }
  }

}
