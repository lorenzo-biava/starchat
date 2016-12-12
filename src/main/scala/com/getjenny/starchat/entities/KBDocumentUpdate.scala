package com.getjenny.starchat.entities

/**
  * Created by Angelo Leto <angelo.leto@elegans.io> on 01/07/16.
  */

case class KBDocumentUpdate(conversation: Option[String],
                            index_in_conversation: Option[Int],
                            question: Option[String],
                            answer: Option[String],
                            verified: Option[Boolean],
                            topics: Option[String],
                            doctype: Option[String],
                            state: Option[String],
                            status: Option[Int]
                   )
