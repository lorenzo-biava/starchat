package com.getjenny.starchat.resources

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 22/05/18.
  */

import com.getjenny.starchat.services.{ConversationLogsService, QuestionAnswerService}

trait ConversationLogsResource extends QuestionAnswerResource {
  override protected[this] val questionAnswerService: QuestionAnswerService = ConversationLogsService
  override protected[this] val routeName: String = "conversation_logs"
}
