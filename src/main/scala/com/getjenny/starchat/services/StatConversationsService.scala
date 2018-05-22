package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 01/07/16.
  */

object StatConversationsService extends QuestionAnswerService {
  override val elasticClient: QuestionAnswerElasticClient = StatConversationsElasticClient
}
