package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 01/07/16.
  */

object KnowledgeBaseElasticClient extends ElasticClient {
  val kbIndexSuffix: String = config.getString("es.kb_index_suffix")
  val queryMinThreshold : Float = config.getDouble("es.kb_query_min_threshold").toFloat
  val queriesScoreMode: String = config.getString("es.kb_nested_score_mode").toLowerCase
  val questionNegativeMinimumMatch: String = config.getString("es.kb_question_negative_minimum_match")
  val questionNegativeBoost: Float = config.getDouble("es.kb_question_negative_boost").toFloat
  val questionExactMatchBoost: Float = config.getDouble("es.kb_question_exact_match_boost").toFloat
}

