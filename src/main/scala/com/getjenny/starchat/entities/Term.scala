package com.getjenny.starchat.entities

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 11/07/17.
  */

import scala.collection.immutable.Map

case class Term(term: String,
                synonyms: Map[String, Double],
                antonyms: Map[String, Double],
                vector: Array[Double]
               )