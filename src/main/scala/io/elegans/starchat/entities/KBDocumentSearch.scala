package io.elegans.starchat.entities

/**
  * Created by Angelo Leto <angelo.leto@elegans.io> on 01/07/16.
  */

case class KBDocumentSearch(from: Option[Int],
                            size: Option[Int],
                            min_score: Option[Float],
                            question: Option[String],
                            index_in_conversation: Option[Int],
                            answer: Option[String],
                            verified: Option[Boolean],
                            doctype: Option[String],
                            conversation: Option[String]
                           )
