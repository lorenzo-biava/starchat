package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 23/08/17.
  */

import akka.event.{Logging, LoggingAdapter}
import com.getjenny.starchat.SCActorSystem
import com.getjenny.starchat.entities.DtReloadTimestamp
import org.elasticsearch.action.get.{GetRequestBuilder, GetResponse}
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.update.UpdateResponse
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.common.xcontent.XContentBuilder
import org.elasticsearch.common.xcontent.XContentFactory._
import org.elasticsearch.index.query.{BoolQueryBuilder, QueryBuilder, QueryBuilders}
import org.elasticsearch.search.sort.SortOrder
import org.elasticsearch.search.SearchHit

import scala.collection.JavaConverters._
import scala.collection.immutable.Map
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.Scalaz._

object  DtReloadService {
  val DT_RELOAD_TIMESTAMP_DEFAULT : Long = -1
  private[this] val elasticClient: SystemIndexManagementElasticClient.type = SystemIndexManagementElasticClient
  private[this] val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)

  private[this] def getIndexName(indexName: Option[String] = None, suffix: Option[String] = None): String = {
    indexName.getOrElse(elasticClient.indexName) + "." + suffix.getOrElse(elasticClient.systemRefreshDtIndexSuffix)
  }

  def setDTReloadTimestamp(indexName: String, refresh: Int = 0):
  Future[Option[DtReloadTimestamp]] = Future {
    val dtReloadDocId: String = indexName
    val timestamp: Long = System.currentTimeMillis

    val builder : XContentBuilder = jsonBuilder().startObject()
    builder.field(elasticClient.dtReloadTimestampFieldName, timestamp)
    builder.endObject()

    val client: TransportClient = elasticClient.client
    val response: UpdateResponse =
      client.prepareUpdate().setIndex(getIndexName())
        .setType(elasticClient.systemRefreshDtIndexSuffix)
        .setId(dtReloadDocId)
        .setDocAsUpsert(true)
        .setDoc(builder)
        .get()

    log.debug("dt reload timestamp response status: " + response.status())

    if (refresh =/= 0) {
      val refreshIndex = elasticClient.refresh(getIndexName())
      if(refreshIndex.failed_shards_n > 0) {
        throw new Exception("System: index refresh failed: (" + indexName + ")")
      }
    }

    Option {DtReloadTimestamp(indexName = indexName, timestamp = timestamp)}
  }

  def getDTReloadTimestamp(indexName: String) : Future[Option[DtReloadTimestamp]] = Future {
    val dtReloadDocId: String = indexName
    val client: TransportClient = elasticClient.client
    val getBuilder: GetRequestBuilder = client.prepareGet()
      .setIndex(getIndexName())
      .setType(elasticClient.systemRefreshDtIndexSuffix)
      .setId(dtReloadDocId)
    val response: GetResponse = getBuilder.get()

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
    val client: TransportClient = elasticClient.client
    val boolQueryBuilder : BoolQueryBuilder = QueryBuilders.boolQuery()
    minTimestamp match {
      case Some(minTs) => boolQueryBuilder.filter(QueryBuilders.rangeQuery("state_refresh_ts").gt(minTs))
      case _ => ;
    }

    val scrollResp : SearchResponse = client.prepareSearch(getIndexName())
      .setTypes(elasticClient.systemRefreshDtIndexSuffix)
      .setQuery(boolQueryBuilder)
      .addSort("state_refresh_ts", SortOrder.DESC)
      .setScroll(new TimeValue(60000))
      .setSize(maxItems.getOrElse(0L).toInt).get()

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
