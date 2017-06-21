package com.getjenny.starchat.entities

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 01/07/16.
  */

case class KBDocumentSearch(from: Option[Int],
                            size: Option[Int],
                            min_score: Option[Float],
                            question: Option[String],
                            question_scored_terms: Option[String],
                            index_in_conversation: Option[Int],
                            answer: Option[String],
                            answer_scored_terms: Option[String],
                            verified: Option[Boolean],
                            doctype: Option[String],
                            conversation: Option[String]
                           )
