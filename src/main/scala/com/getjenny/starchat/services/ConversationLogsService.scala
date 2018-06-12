package com.getjenny.starchat.services

import com.getjenny.starchat.services.esclient.{ConversationLogsElasticClient, QuestionAnswerElasticClient}

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 22/05/18.
  */

object ConversationLogsService extends QuestionAnswerService {
  override val elasticClient: QuestionAnswerElasticClient = ConversationLogsElasticClient
}
