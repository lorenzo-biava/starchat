package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 23/08/17.
  */

import akka.event.{Logging, LoggingAdapter}
import com.getjenny.starchat.SCActorSystem
import com.getjenny.starchat.entities.DtReloadTimestamp
import com.getjenny.starchat.services.esclient.SystemIndexManagementElasticClient
import com.getjenny.starchat.utils.Index
import org.elasticsearch.action.get.{GetRequest, GetResponse}
import org.elasticsearch.action.search.{SearchRequest, SearchResponse}
import org.elasticsearch.action.update.{UpdateRequest, UpdateResponse}
import org.elasticsearch.client.{RequestOptions, RestHighLevelClient}
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.common.xcontent.XContentBuilder
import org.elasticsearch.common.xcontent.XContentFactory._
import org.elasticsearch.index.query.{BoolQueryBuilder, QueryBuilders}
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.sort.SortOrder
import scalaz.Scalaz._

import scala.collection.JavaConverters._
import scala.collection.immutable.Map
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object  DtReloadService {
  val DT_RELOAD_TIMESTAMP_DEFAULT : Long = -1
  private[this] val elasticClient: SystemIndexManagementElasticClient.type = SystemIndexManagementElasticClient
  private[this] val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)

  def setDTReloadTimestamp(indexName: String, refresh: Int = 0):
  Future[Option[DtReloadTimestamp]] = Future {
    val client: RestHighLevelClient = elasticClient.client
    val dtReloadDocId: String = indexName
    val timestamp: Long = System.currentTimeMillis

    val builder : XContentBuilder = jsonBuilder().startObject()
    builder.field(elasticClient.dtReloadTimestampFieldName, timestamp)
    builder.endObject()

    val updateReq = new UpdateRequest()
      .index(Index.indexName(elasticClient.indexName, elasticClient.systemRefreshDtIndexSuffix))
      .`type`(elasticClient.systemRefreshDtIndexSuffix)
      .doc(builder)
      .id(dtReloadDocId)
      .docAsUpsert(true)

    val response: UpdateResponse = client.update(updateReq, RequestOptions.DEFAULT)

    log.debug("dt reload timestamp response status: " + response.status())

    if (refresh =/= 0) {
      val refreshIndex = elasticClient
        .refresh(Index.indexName(elasticClient.indexName, elasticClient.systemRefreshDtIndexSuffix))
      if(refreshIndex.failed_shards_n > 0) {
        throw new Exception("System: index refresh failed: (" + indexName + ")")
      }
    }

    Option {DtReloadTimestamp(indexName = indexName, timestamp = timestamp)}
  }

  def getDTReloadTimestamp(indexName: String) : Future[Option[DtReloadTimestamp]] = Future {
    val client: RestHighLevelClient = elasticClient.client
    val dtReloadDocId: String = indexName

    val getReq = new GetRequest()
      .`type`(elasticClient.systemRefreshDtIndexSuffix)
      .index(Index.indexName(elasticClient.indexName, elasticClient.systemRefreshDtIndexSuffix))
      .id(dtReloadDocId)

    val response: GetResponse = client.get(getReq, RequestOptions.DEFAULT)

    val timestamp = if(! response.isExists || response.isSourceEmpty) {
      log.info("dt reload timestamp field is empty or does not exists")
      DT_RELOAD_TIMESTAMP_DEFAULT
    } else {
      val source : Map[String, Any] = response.getSource.asScala.toMap
      val loadedTs : Long = source.get(elasticClient.dtReloadTimestampFieldName) match {
        case Some(t) => t.asInstanceOf[Long]
        case None =>
          DT_RELOAD_TIMESTAMP_DEFAULT
      }
      log.info("dt reload timestamp is: " + loadedTs)
      loadedTs
    }

    Option {DtReloadTimestamp(indexName = indexName, timestamp = timestamp)}
  }

  def allDTReloadTimestamp(minTimestamp: Option[Long] = None,
                           maxItems: Option[Long] = None): Option[List[DtReloadTimestamp]] = {
    val client: RestHighLevelClient = elasticClient.client
    val boolQueryBuilder : BoolQueryBuilder = QueryBuilders.boolQuery()
    minTimestamp match {
      case Some(minTs) => boolQueryBuilder.filter(QueryBuilders.rangeQuery("state_refresh_ts").gt(minTs))
      case _ => ;
    }

    val sourceReq: SearchSourceBuilder = new SearchSourceBuilder()
      .query(boolQueryBuilder)
      .size(maxItems.getOrElse(100L).toInt)
      .version(true)
      .sort("state_refresh_ts", SortOrder.DESC)

    val searchReq = new SearchRequest(Index.indexName(elasticClient.indexName, elasticClient.systemRefreshDtIndexSuffix))
      .source(sourceReq)
      .types(elasticClient.systemRefreshDtIndexSuffix)
      .scroll(new TimeValue(60000))

    val scrollResp : SearchResponse = client.search(searchReq, RequestOptions.DEFAULT)

    val dtReloadTimestamps : List[DtReloadTimestamp] = scrollResp.getHits.getHits.toList.map({ timestampEntry =>
      val item: SearchHit = timestampEntry
      val docId : String = item.getId // the id is the index name
    val source : Map[String, Any] = item.getSourceAsMap.asScala.toMap

      val timestamp : Long = source.get("state_refresh_ts") match {
        case Some(t) => t.asInstanceOf[Long]
        case None => DT_RELOAD_TIMESTAMP_DEFAULT
      }

      DtReloadTimestamp(docId, timestamp)
    })
    Option {dtReloadTimestamps}
  }

}
