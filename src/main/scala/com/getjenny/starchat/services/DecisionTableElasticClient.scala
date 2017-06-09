package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 01/07/16.
  */

object DecisionTableElasticClient extends ElasticClient {
  val type_name: String = config.getString("es.dt_type_name")
  val query_min_threshold : Float = config.getDouble("es.dt_query_min_threshold").toFloat
  val boost_exact_match_factor : Float = config.getDouble("es.dt_boost_exact_match_factor").toFloat
  val queries_score_mode: String = config.getString("es.dt_queries_score_mode").toLowerCase
}
