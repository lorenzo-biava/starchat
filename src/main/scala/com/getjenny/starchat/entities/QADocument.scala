package com.getjenny.starchat.entities

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 01/07/16.
  */


object Doctypes {
  val normal: String = "normal" /* normal document, can be returned to the user as response */
  val canned: String = "canned" /* canned document, indexed but retrieved only under particular circumstances*/
  val hidden: String = "hidden" /* hidden document, these are indexed but must not be retrieved,
                                      use this type for data used just to improve statistic for data retrieval */
  val decisiontable: String = "decisiontable" /* does not contains conversation data, used to redirect the
                                                    conversation to any state of the decision tree */
}

case class QADocument(id: String, /* unique id of the document */
                      conversation: String, /* ID of the conversation (multiple q&a may be inside a conversation) */
                      indexInConversation: Option[Int] = None, /* the index of the document in the conversation flow */
                      question: String, /* usually what the user of the chat says */
                      questionNegative: Option[List[String]] = None, /* list of sentences different to the main question */
                      questionScoredTerms: Option[List[(String, Double)]] = None, /* terms list in form {"term": "<term>", "score": 0.2121} */
                      answer: String, /* usually what the operator of the chat says */
                      answerScoredTerms: Option[List[(String, Double)]] = None, /* terms list in form {"term": "<term>", "score": 0.2121} */
                      verified: Boolean = false, /* was the conversation verified by an operator? */
                      topics: Option[String] = None, /* list of topics */
                      dclass: Option[String] = None, /* document classes e.g. group0 group1 etc.*/
                      doctype: String = Doctypes.normal, /* document type */
                      state: Option[String] = None, /* eventual link to any of the state machine states */
                      status: Int = 0 /* tell whether the document is locked for editing or not, useful for
                                              a GUI to avoid concurrent modifications, 0 means no operations pending */
                   )
