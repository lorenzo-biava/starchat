package com.getjenny.starchat.services.esclient

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 01/07/16.
  */

object KnowledgeBaseElasticClient extends QuestionAnswerElasticClient {
  override val indexSuffix: String = kbIndexSuffix
  override val indexMapping: String = "question_answer"
  override val dictSizeCacheMaxSize: Int = config.getInt("es.dictSizeCacheMaxSize")
  override val totalTermsCacheMaxSize: Int = config.getInt("es.totalTermsCacheMaxSize")
  override val countTermCacheMaxSize: Int = config.getInt("es.countTermCacheMaxSize")
  override val cacheStealTimeMillis: Int = config.getInt("es.cacheStealTimeMillis")
}


