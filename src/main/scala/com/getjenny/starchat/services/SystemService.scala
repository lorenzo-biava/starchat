package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 23/08/17.
  */

import java.util

import akka.actor.ActorSystem
import com.getjenny.starchat.entities._

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.collection.immutable.{List, Map}
import org.elasticsearch.common.xcontent.XContentBuilder
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.xcontent.XContentFactory._
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.action.update.UpdateResponse
import org.elasticsearch.action.delete.DeleteResponse
import org.elasticsearch.action.get.{GetResponse, MultiGetItemResponse, GetRequestBuilder, MultiGetRequestBuilder, MultiGetResponse}
import org.elasticsearch.action.search.{SearchRequestBuilder, SearchResponse, SearchType}
import org.elasticsearch.index.query.{BoolQueryBuilder, QueryBuilder, QueryBuilders, InnerHitBuilder}
import org.elasticsearch.common.unit._

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import org.elasticsearch.search.SearchHit
import org.elasticsearch.rest.RestStatus
import com.getjenny.starchat.analyzer.analyzers._

import scala.util.{Failure, Success, Try}
import akka.event.{Logging, LoggingAdapter}
import akka.event.Logging._
import com.getjenny.starchat.SCActorSystem
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse
import org.apache.lucene.search.join._
import scala.concurrent.ExecutionContext.Implicits.global

object SystemService {
  var dt_reload_timestamp : Long = -1
  val elastic_client = SystemElasticClient
  val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)
  val dtReloadDocId: String = "dtts0"

  def setDTReloadTimestamp(index_name: String, refresh: Int = 0):
      Future[Option[Long]] = Future {
    val dt_reload_doc_id: String = index_name + "_" + dtReloadDocId
    val timestamp: Long = System.currentTimeMillis

    val builder : XContentBuilder = jsonBuilder().startObject()
    builder.field(elastic_client.dt_reload_timestamp_field_name, timestamp)
    builder.endObject()

    val client: TransportClient = elastic_client.get_client()
    val response: UpdateResponse =
      client.prepareUpdate(index_name, elastic_client.type_name, dt_reload_doc_id)
      .setDocAsUpsert(true)
      .setDoc(builder)
      .get()

    log.debug("dt reload timestamp response status: " + response.status())

    if (refresh != 0) {
      val refresh_index = elastic_client.refresh_index(index_name)
      if(refresh_index.failed_shards_n > 0) {
        throw new Exception("System: index refresh failed: (" + index_name + ")")
      }
    }

    Option {timestamp}
  }

  def getDTReloadTimestamp(index_name: String) : Future[Option[Long]] = Future {
    val dt_reload_doc_id: String = index_name + "_" + dtReloadDocId
    val client: TransportClient = elastic_client.get_client()
    val get_builder: GetRequestBuilder = client.prepareGet()
    get_builder.setIndex(index_name).setType(elastic_client.type_name).setId(dt_reload_doc_id)
    val response: GetResponse = get_builder.get()

    val timestamp = if(! response.isExists || response.isSourceEmpty) {
      log.info("dt reload timestamp field is empty or does not exists")
      -1 : Long
    } else {
      val source : Map[String, Any] = response.getSource.asScala.toMap
      val loaded_ts : Long = source.get(elastic_client.dt_reload_timestamp_field_name) match {
        case Some(t) => t.asInstanceOf[Long]
        case None =>
          -1: Long
      }
      log.info("dt reload timestamp is: " + loaded_ts)
      loaded_ts
    }

    Option {timestamp}
  }

}
