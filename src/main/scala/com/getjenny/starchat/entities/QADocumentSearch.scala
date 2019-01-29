package com.getjenny.starchat.entities

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 01/07/16.
  */

case class QADocumentSearch(from: Option[Int],
                            size: Option[Int],
                            minScore: Option[Float],
                            question: Option[String],
                            questionScoredTerms: Option[String],
                            indexInConversation: Option[Int],
                            answer: Option[String],
                            answerScoredTerms: Option[String],
                            verified: Option[Boolean],
                            doctype: Option[String],
                            conversation: Option[String],
                            topics: Option[String],
                            dclass: Option[String],
                            state: Option[String],
                            status: Option[Int],
                            random: Option[Boolean]
                           )
