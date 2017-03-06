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

object  DecisionTableElasticClient extends ElasticClient {
  override val type_name = config.getString("es.dt_type_name")
  override val query_min_threshold : Float = config.getDouble("es.dt_query_min_threshold").toFloat
  val boost_exact_match_factor : Float = config.getDouble("es.dt_boost_exact_match_factor").toFloat

}

