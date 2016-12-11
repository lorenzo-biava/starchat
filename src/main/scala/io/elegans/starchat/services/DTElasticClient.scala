package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo.leto@elegans.io> on 01/07/16.
  */

import java.net.InetAddress

import com.typesafe.config.ConfigFactory
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.{InetSocketTransportAddress, TransportAddress}
import scala.collection.JavaConversions._
import scala.collection.immutable.{List, Map}

object  DTElasticClient {
  val config = ConfigFactory.load()
  val index_name = config.getString("es.index_name")
  val type_name = config.getString("es.dt_type_name")
  val cluster_name = config.getString("es.cluster_name")
  val ignore_cluster_name = config.getBoolean("es.ignore_cluster_name")
  val query_min_threshold : Float = config.getDouble("es.dt_query_min_threshold").toFloat

  val host_map : Map[String, Int] = config.getAnyRef("es.host_map").asInstanceOf[java.util.HashMap[String,Int]] toMap

  val settings: Settings = Settings.builder()
    .put("cluster.name", cluster_name)
    .put("client.transport.ignore_cluster_name", ignore_cluster_name)
    .put("client.transport.sniff", true).build()

  val inet_addresses: List[TransportAddress] =
    host_map.map{ case(k,v) => new InetSocketTransportAddress(InetAddress.getByName(k), v) } toList

  var client : TransportClient = open_client()

  def open_client(): TransportClient = {
    val client: TransportClient = TransportClient.builder().settings(settings).build()
      .addTransportAddresses(inet_addresses:_*)
    client
  }

  def get_client(): TransportClient = {
    return this.client
  }

  def close_client(client: TransportClient): Unit = {
    client.close()
  }
}

