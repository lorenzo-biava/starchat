package com.getjenny.starchat.resources

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 22/05/18.
  */

import com.getjenny.starchat.services.{QuestionAnswerService, StatTextService}

trait StatTextResource extends QuestionAnswerResource {
  override protected[this] val questionAnswerService: QuestionAnswerService = StatTextService
  override protected[this] val routeName: String = "stat_text"
}
