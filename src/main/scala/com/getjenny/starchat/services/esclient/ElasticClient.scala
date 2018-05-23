package com.getjenny.starchat.services.esclient

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 01/07/16.
  */

import java.net.InetAddress

import com.getjenny.starchat.entities._
import com.typesafe.config.{Config, ConfigFactory}
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.TransportAddress
import org.elasticsearch.transport.client.PreBuiltTransportClient

import scala.collection.immutable.{List, Map}

trait ElasticClient {
  val config: Config = ConfigFactory.load()
  val clusterName: String = config.getString("es.cluster_name")
  val ignoreClusterName: Boolean = config.getBoolean("es.ignore_cluster_name")

  val hostMapStr : String = config.getString("es.host_map")
  val hostMap : Map[String, Int] = hostMapStr.split(";").map(x => x.split("=")).map(x => (x(0), (x(1)).toInt)).toMap

  val settings: Settings = Settings.builder()
    .put("cluster.name", clusterName)
    .put("client.transport.ignore_cluster_name", ignoreClusterName)
    .put("client.transport.sniff", false).build()

  val inetAddresses: List[TransportAddress] =
    hostMap.map{ case(k,v) => new TransportAddress(InetAddress.getByName(k), v) }.toList

  private[this] var transportClient : TransportClient = open()

  def open(): TransportClient = {
    val client: TransportClient = new PreBuiltTransportClient(settings)
      .addTransportAddresses(inetAddresses:_*)
    client
  }

  def refresh(indexName: String): RefreshIndexResult = {
    val refreshRes: RefreshResponse =
      transportClient.admin().indices().prepareRefresh(indexName).get()

    val failedShards: List[FailedShard] = refreshRes.getShardFailures.map(item => {
      val failedShardItem = FailedShard(index_name = item.index,
        shard_id = item.shardId,
        reason = item.reason,
        status = item.status.getStatus
      )
      failedShardItem
    }).toList

    val refreshIndexResult =
      RefreshIndexResult(index_name = indexName,
        failed_shards_n = refreshRes.getFailedShards,
        successful_shards_n = refreshRes.getSuccessfulShards,
        total_shards_n = refreshRes.getTotalShards,
        failed_shards = failedShards
      )
    refreshIndexResult
  }

  def client: TransportClient = {
    this.transportClient
  }

  def close(client: TransportClient): Unit = {
    client.close()
  }

  val convLogsIndexSuffix: String = config.getString("es.conv_logs_index_suffix")
  val dtIndexSuffix: String = config.getString("es.dt_index_suffix")
  val kbIndexSuffix: String = config.getString("es.kb_index_suffix")
  val statTextIndexSuffix: String = config.getString("es.stat_text_index_suffix")
  val sysRefreshDtIndexSuffix: String = config.getString("es.system_refresh_dt_index_suffix")
  val systemRefreshDtIndexSuffix: String = config.getString("es.system_refresh_dt_index_suffix")
  val termIndexSuffix: String = config.getString("es.term_index_suffix")
  val userIndexSuffix: String = config.getString("es.user_index_suffix")

  val enableDeleteIndex: Boolean = config.getBoolean("es.enable_delete_application_index")
  val enableDeleteSystemIndex: Boolean = config.getBoolean("es.enable_delete_system_index")
}
