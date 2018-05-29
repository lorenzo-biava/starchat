package com.getjenny.starchat.services.esclient

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 01/07/16.
  */

object PriorConversationsElasticClient extends QuestionAnswerElasticClient {
  override val indexSuffix: String = priorDataIndexSuffix
}
