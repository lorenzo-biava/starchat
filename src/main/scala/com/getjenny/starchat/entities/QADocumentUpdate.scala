package com.getjenny.starchat.entities

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 01/07/16.
  */

case class QADocumentCoreUpdate(
                                 question: Option[String] = None , /* usually what the user of the chat says */
                                 questionNegative: Option[List[String]] = None, /* list of sentences different to the main question */
                                 questionScoredTerms: Option[List[(String, Double)]] = None, /* terms list in form {"term": "<term>", "score": 0.2121} */
                                 answer: Option[String] = None, /* usually what the operator of the chat says */
                                 answerScoredTerms: Option[List[(String, Double)]] = None, /* terms list in form {"term": "<term>", "score": 0.2121} */
                                 topics: Option[String] = None, /* list of topics */
                                 verified: Option[Boolean] = None, /* was the conversation verified by an operator? */
                                 done: Option[Boolean] = None /* mark the conversation as done, this field is expected to set once for each conversation */
                               )

case class QADocumentAnnotationsUpdate(
                                        dclass: Option[String] = None, /* document classes e.g. group0 group1 etc.*/
                                        doctype: Option[Doctypes.Value] = None, /* document type */
                                        state: Option[String] = None, /* eventual link to any of the state machine states */
                                        agent: Option[Agent.Value] = None,
                                        escalated: Option[Escalated.Value] = None,
                                        answered: Option[Answered.Value] = None,
                                        triggered: Option[Triggered.Value] = None,
                                        followup: Option[Followup.Value] = None,
                                        feedbackConv: Option[String] = None, /* A feedback provided by the user to the conversation */
                                        feedbackConvScore: Option[Double] = None, /* a field to store the score provided by the user to the conversation */
                                        algorithmConvScore: Option[Double] = None, /* a field to store the score calculated by an algorithm related to the conversation i.e. a sentiment
analysis tool (for future use) */
                                        feedbackAnswerScore: Option[Double] = None, /* description: a field to store the score provided by the user for the answer */
                                        algorithmAnswerScore: Option[Double] = None, /* a field to store the score calculated by an algorithm related to the answer i.e. a sentiment
analysis tool (for future use) */
                                        start: Option[Boolean] = None, /* event determined when a start state is loaded */
                                      )

case class QADocumentUpdate(
                             id: List[String], /* list of document ids to update (bulk editing) */
                             conversation: Option[String] = None, /* ID of the conversation (multiple q&a may be inside a conversation) */
                             indexInConversation: Option[Int] = None, /* the index of the document in the conversation flow */
                             coreData: Option[QADocumentCoreUpdate] = None, /* core question answer fields */
                             annotations: Option[QADocumentAnnotationsUpdate] = None, /* qa and conversation annotations */
                             status: Option[Int] = None, /* tell whether the document is locked for editing or not, useful for
                                              a GUI to avoid concurrent modifications, 0 means no operations pending */
                             timestamp: Option[Long] = None /* indexing timestamp, automatically calculated if not provided */
                           )