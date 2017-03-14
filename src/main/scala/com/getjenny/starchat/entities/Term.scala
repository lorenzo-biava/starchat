package com.getjenny.starchat.entities

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 11/07/17.
  */

import scala.collection.immutable.{Map}

case class Term(term: String,
                synonyms: Option[Map[String, Double]],
                antonyms: Option[Map[String, Double]],
                frequency: Option[Double],
                vector: Option[Vector[Double]],
                score: Option[Double]
               )

case class Terms(terms: List[Term])

case class TermIdsRequest(ids: List[String])

case class TermsResults(total: Int, max_score: Float, hits: Terms)
