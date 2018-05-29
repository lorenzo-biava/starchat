package com.getjenny.starchat.services

import com.getjenny.starchat.services.esclient.{QuestionAnswerElasticClient, PriorConversationsElasticClient}

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 01/07/16.
  */

object PriorDataService extends QuestionAnswerService {
  override val elasticClient: QuestionAnswerElasticClient = PriorConversationsElasticClient
}
