package com.getjenny.starchat.entities

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 01/07/16.
  */

case class KBDocumentUpdate(conversation: Option[String] = None,
                            indexInConversation: Option[Int] = None,
                            question: Option[String] = None,
                            questionNegative: Option[List[String]] = None,
                            questionScoredTerms: Option[List[(String, Double)]] = None,
                            answer: Option[String] = None,
                            answerScoredTerms: Option[List[(String, Double)]] = None,
                            verified: Option[Boolean] = None,
                            topics: Option[String] = None,
                            dclass: Option[String] = None,
                            doctype: Option[String] = None,
                            state: Option[String] = None,
                            status: Option[Int] = None
                   )
