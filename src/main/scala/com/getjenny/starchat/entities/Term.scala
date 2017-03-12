package com.getjenny.starchat.entities

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 11/07/17.
  */

import scala.collection.immutable.{Map}

case class Term(id:String,
                term: String,
                synonyms: Map[String, Double],
                antonyms: Map[String, Double],
                vector: Array[Double],
                score: Option[Double]
               )

case class TermGetRequest(ids: Seq[String], terms: Seq[String])

case class TermsResults(total: Int, max_score: Float, hits: Seq[Term])
