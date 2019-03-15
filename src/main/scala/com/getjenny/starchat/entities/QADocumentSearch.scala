package com.getjenny.starchat.entities

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 01/07/16.
  */


case class QADocumentSearch(
                             from: Option[Int] = None,
                             size: Option[Int] = None,
                             minScore: Option[Float] = None,
                             sortByConvIdIdx: Option[Boolean] = None,

                             conversation: Option[List[String]] = None, /* IDs of the conversations (or query) */
                             indexInConversation: Option[Int] = None, /* the index of the document in the conversation flow */
                             coreData: Option[QADocumentCoreUpdate] = None, /* core question answer fields */
                             annotations: Option[QADocumentAnnotationsUpdate] = None, /* qa and conversation annotations */
                             status: Option[Int] = None, /* tell whether the document is locked for editing or not, useful for
                                              a GUI to avoid concurrent modifications, 0 means no operations pending */

                             timestampGte: Option[Long] = None, /* min indexing timestamp, None means no lower bound */
                             timestampLte: Option[Long] = None, /* max indexing timestamp, None means no upper bound*/
                             random: Option[Boolean] = None
                           )