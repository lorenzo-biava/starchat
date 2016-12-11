package com.getjenny.starchat.entities

import scala.collection.immutable.{List}

/**
  * Created by Angelo Leto <angelo.leto@elegans.io> on 01/07/16.
  */

case class SearchDTDocument(score: Float, document: DTDocument)

case class SearchDTDocumentsResults(total: Int, max_score: Float, hits: List[SearchDTDocument])

