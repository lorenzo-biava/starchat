package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 01/07/16.
  */

object KnowledgeBaseElasticClient extends ElasticClient {
  val kb_index_suffix: String = config.getString("es.kb_index_suffix")
  val query_min_threshold : Float = config.getDouble("es.kb_query_min_threshold").toFloat
  val queries_score_mode: String = config.getString("es.kb_nested_score_mode").toLowerCase
  val question_negative_minimum_match: String = config.getString("es.kb_question_negative_minimum_match")
  val question_negative_boost: Float = config.getDouble("es.kb_question_negative_boost").toFloat
  val question_exact_match_boost: Float = config.getDouble("es.kb_question_exact_match_boost").toFloat
}

