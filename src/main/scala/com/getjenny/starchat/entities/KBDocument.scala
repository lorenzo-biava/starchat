package com.getjenny.starchat.entities

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 01/07/16.
  */


object doctypes {
  val normal: String = "normal" /* normal document, can be returned to the user as response */
  val hidden: String = "hidden" /* hidden document, these are indexed but must not be retrieved,
                                      use this type for data used just to improve statistic for data retrieval */
  val decisiontable: String = "decisiontable" /* does not contains conversation data, used to redirect the
                                                    conversation to any state of the decision tree */
}

case class KBDocument(id: String, /* unique id of the document */
                      conversation: String, /* ID of the conversation (multiple q&a may be inside a conversation) */
                      index_in_conversation: Option[Int], /* the index of the document in the conversation flow */
                      question: String, /* usually what the user of the chat says */
                      question_scored_terms: Option[List[(String, Double)]], /* terms list in form {"term": "<term>", "score": 0.2121} */
                      answer: String, /* usually what the operator of the chat says */
                      answer_scored_terms: Option[List[(String, Double)]], /* terms list in form {"term": "<term>", "score": 0.2121} */
                      verified: Boolean = false, /* was the conversation verified by an operator? */
                      topics: Option[String], /* list of topics */
                      doctype: String = doctypes.normal, /* document type */
                      state: Option[String], /* eventual link to any of the state machine states */
                      status: Int = 0 /* tell whether the document is locked for editing or not, useful for
                                              a GUI to avoid concurrent modifications, 0 means no operations pending */
                   )
