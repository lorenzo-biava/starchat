package com.getjenny.starchat.entities

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 01/07/16.
  */


case class QADocumentSearch(
                             from: Option[Int],
                             size: Option[Int],
                             minScore: Option[Float],
                             sortByConvIdIdx: Option[Boolean],

                             conversation: Option[String] = None, /* ID of the conversation (multiple q&a may be inside a conversation) */
                             indexInConversation: Option[Int] = None, /* the index of the document in the conversation flow */
                             coreData: Option[QADocumentCoreUpdate] = None, /* core question answer fields */
                             annotations: Option[QADocumentAnnotationsUpdate] = None, /* qa and conversation annotations */
                             status: Option[Int] = None, /* tell whether the document is locked for editing or not, useful for
                                              a GUI to avoid concurrent modifications, 0 means no operations pending */

                             timestampGte: Option[Long] = None, /* min indexing timestamp, None means no lower bound */
                             timestampLte: Option[Long] = None, /* max indexing timestamp, None means no upper bound*/
                             random: Option[Boolean]
                           )