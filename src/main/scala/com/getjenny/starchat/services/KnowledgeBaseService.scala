package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 01/07/16.
  */

object KnowledgeBaseService extends QuestionAnswerService {
  override val elasticClient: QuestionAnswerElasticClient = KnowledgeBaseElasticClient
}
