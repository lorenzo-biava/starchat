package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 22/05/18.
  */

object ConversationLogsElasticClient extends QuestionAnswerElasticClient {
  override val indexSuffix: String = convLogsIndexSuffix
}
