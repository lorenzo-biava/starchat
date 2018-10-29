package com.getjenny.starchat.services

import com.getjenny.starchat.entities.{DeleteDocumentResult, DeleteDocumentsResult, DeleteDocumentsSummaryResult, DocsIds}
import com.getjenny.starchat.services.esclient.ElasticClient
import com.getjenny.starchat.utils.Index
import org.elasticsearch.action.bulk.{BulkRequest, BulkResponse}
import org.elasticsearch.action.delete.DeleteRequest
import org.elasticsearch.action.search.{SearchRequest, SearchResponse}
import org.elasticsearch.client.{RequestOptions, RestHighLevelClient}
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.rest.RestStatus
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.builder.SearchSourceBuilder
import scalaz.Scalaz._

import scala.collection.immutable.List
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

case class DeleteDataServiceException(message: String = "", cause: Throwable = None.orNull)
  extends Exception(message, cause)

trait AbstractDataService {

  val elasticClient: ElasticClient

  /** delete all the terms in a table
    *
    * @param indexName index name
    * @return a DeleteDocumentsResult with the status of the delete operation
    */
  def deleteAll(indexName: String): Future[DeleteDocumentsSummaryResult] = Future {
    val client: RestHighLevelClient = elasticClient.client

    /* TODO: to be implemented with deleteByQuery when it will be integrated in ES 7.0 */
    val sourceReq: SearchSourceBuilder = new SearchSourceBuilder()
      .query(QueryBuilders.matchAllQuery)
      .fetchSource(Array.empty[String], Array.empty[String])
      .size(0)

    val searchReq = new SearchRequest(Index.indexName(indexName, elasticClient.indexSuffix))
      .source(sourceReq)
      .types(elasticClient.indexSuffix)
      .scroll(new TimeValue(60000))

    var scrollResp: SearchResponse = client.search(searchReq, RequestOptions.DEFAULT)
    val iterator = Iterator.continually {
      scrollResp.getHits.getHits.toList.map {
        case e: SearchHit => e.getId
      }
    }.takeWhile {
      case idsList: List[String] => idsList.nonEmpty
    }

    val deleted = iterator.map { case ids: List[String] =>
      Await.result(delete(indexName, DocsIds(ids = ids), 0).map {
        case deleteDocRes: DeleteDocumentsResult =>
          deleteDocRes.data.map(delItem => delItem.found match {
            case true => 1;
            case _ => 0
          }).sum
      }, 10.second)
    }.sum

    DeleteDocumentsSummaryResult(message = "delete", deleted = deleted)
  }

  /** delete one or more terms
    *
    * @param indexName index name
    * @param docIds the list of term ids to delete
    * @param refresh whether to call an index update on ElasticSearch or not
    * @return DeleteDocumentListResult with the result of term delete operations
    */
  def delete(indexName: String, docIds: DocsIds, refresh: Int): Future[DeleteDocumentsResult] = Future {
    val client: RestHighLevelClient = elasticClient.client

    val bulkReq : BulkRequest = new BulkRequest()

    docIds.ids.foreach( id => {
      val deleteReq = new DeleteRequest()
        .index(Index.indexName(indexName, elasticClient.indexSuffix))
        .`type`(elasticClient.indexSuffix)
        .id(id)
      bulkReq.add(deleteReq)
    })

    val bulkRes: BulkResponse = client.bulk(bulkReq, RequestOptions.DEFAULT)

    if (refresh =/= 0) {
      val refreshIndex = elasticClient
        .refresh(Index.indexName(indexName, elasticClient.indexSuffix))
      if(refreshIndex.failed_shards_n > 0) {
        throw DeleteDataServiceException("index refresh failed: (" + indexName + ")")
      }
    }

    val listOfDocRes: List[DeleteDocumentResult] = bulkRes.getItems.map(x => {
      DeleteDocumentResult(x.getIndex, x.getType, x.getId,
        x.getVersion,
        x.status =/= RestStatus.NOT_FOUND)
    }).toList

    DeleteDocumentsResult(data=listOfDocRes)
  }
}
