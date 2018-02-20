package com.getjenny.starchat.entities

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 11/07/17.
  */

import scala.collection.immutable.Map

case class Term(term: String,
                synonyms: Option[Map[String, Double]] = None,
                antonyms: Option[Map[String, Double]] = None,
                tags: Option[String] = None,
                features: Option[Map[String, String]] = None,
                frequency_base: Option[Double] = None,
                frequency_stem: Option[Double] = None,
                vector: Option[Vector[Double]] = None,
                score: Option[Double] = None
               )

case class SearchTerm(term: Option[String] = None,
                synonyms: Option[Map[String, Double]] = None,
                antonyms: Option[Map[String, Double]] = None,
                tags: Option[String] = None,
                features: Option[Map[String, String]] = None,
                frequency_base: Option[Double] = None,
                frequency_stem: Option[Double] = None,
                vector: Option[Vector[Double]] = None,
                score: Option[Double] = None
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