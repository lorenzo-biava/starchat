package com.getjenny.starchat.services

import com.getjenny.starchat.services.esclient.{QuestionAnswerElasticClient, StatConversationsElasticClient}

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 01/07/16.
  */

object StatTextService extends QuestionAnswerService {
  override val elasticClient: QuestionAnswerElasticClient = StatConversationsElasticClient
}
