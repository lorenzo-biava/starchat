package com.getjenny.starchat.entities

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 01/07/16.
  */

case class DTDocumentSearch(from: Option[Int],
                            size: Option[Int],
                            min_score: Option[Float],
                            state: Option[String],
                            queries: Option[String]
                           )
