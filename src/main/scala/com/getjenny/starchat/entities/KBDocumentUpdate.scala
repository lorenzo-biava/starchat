package com.getjenny.starchat.entities

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 01/07/16.
  */

case class KBDocumentUpdate(conversation: Option[String] = None,
                            index_in_conversation: Option[Int] = None,
                            question: Option[String] = None,
                            question_negative: Option[List[String]] = None,
                            question_scored_terms: Option[List[(String, Double)]] = None,
                            answer: Option[String] = None,
                            answer_scored_terms: Option[List[(String, Double)]] = None,
                            verified: Option[Boolean] = None,
                            topics: Option[String] = None,
                            dclass: Option[String] = None,
                            doctype: Option[String] = None,
                            state: Option[String] = None,
                            status: Option[Int] = None
                   )
