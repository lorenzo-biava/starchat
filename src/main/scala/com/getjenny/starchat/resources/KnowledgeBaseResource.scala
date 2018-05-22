package com.getjenny.starchat.resources

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 22/05/18.
  */

import com.getjenny.starchat.services.{KnowledgeBaseService, QuestionAnswerService}

trait KnowledgeBaseResource extends QuestionAnswerResource {
  override protected[this] val questionAnswerService: QuestionAnswerService = KnowledgeBaseService
  override protected[this] val routeName: String = "knowledgebase"
}
