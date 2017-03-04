package com.getjenny.starchat.analyzer

/**
  * Created by mal on 20/02/2017.
  */
abstract class Expression {
  def evaluate(query: String): Double  // read a sentence and produce a score (the higher, the more confident)
  val match_threshold = 0.0
  def matches(query: String): Boolean = this.evaluate(query) > this.match_threshold // read a sentence and tells if there is any match
}
