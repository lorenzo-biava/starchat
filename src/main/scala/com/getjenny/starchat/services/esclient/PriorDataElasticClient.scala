package com.getjenny.starchat.services.esclient

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 01/07/16.
  */

object PriorDataElasticClient extends QuestionAnswerElasticClient {
  override val indexSuffix: String = priorDataIndexSuffix
  override val indexMapping: String = "question"
  override val dictSizeCacheMaxSize: Int = config.getInt("es.dictSizeCacheMaxSize")
  override val totalTermsCacheMaxSize: Int = config.getInt("es.totalTermsCacheMaxSize")
  override val countTermCacheMaxSize: Int = config.getInt("es.countTermCacheMaxSize")
}
