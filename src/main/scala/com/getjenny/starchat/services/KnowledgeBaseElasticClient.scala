package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 01/07/16.
  */

import java.net.InetAddress

import com.typesafe.config.ConfigFactory
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.transport.client.PreBuiltTransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.{InetSocketTransportAddress, TransportAddress}
import scala.collection.immutable.{List, Map}
import scala.collection.JavaConverters._

object  KnowledgeBaseElasticClient extends ElasticClient {

  override val type_name = config.getString("es.kb_type_name")
  override val query_min_threshold : Float = config.getDouble("es.kb_query_min_threshold").toFloat

}

