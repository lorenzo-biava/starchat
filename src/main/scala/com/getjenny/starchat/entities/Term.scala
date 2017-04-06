package com.getjenny.starchat.entities

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 11/07/17.
  */

import scala.collection.immutable.{Map}

case class Term(term: String,
                synonyms: Option[Map[String, Double]],
                antonyms: Option[Map[String, Double]],
                tags: Option[String],
                features: Option[Map[String, String]],
                frequency_base: Option[Double],
                frequency_stem: Option[Double],
                vector: Option[Vector[Double]],
                score: Option[Double]
               )

case class Terms(terms: List[Term])

case class TermIdsRequest(ids: List[String])

case class TermsResults(total: Int, max_score: Float, hits: Terms)

case class TextTerms(
                  text: String,
                  text_terms_n: Int,
                  terms_found_n: Int,
                  terms: Option[Terms]
                  )