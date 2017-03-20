package com.getjenny.analyzer.expressions

/**
  * Created by mal on 20/02/2017.
  */
abstract class Expression {
  def evaluate(query: String): Double  // read a sentence and produce a score (the higher, the more confident)
  val match_threshold = 0.0
  def matches(query: String): Boolean = {
    val res = this.evaluate(query) > this.match_threshold
    if (res) println("DEBUG: Expression: " + this + " matches " + query)
    res
  } // read a sentence and tells if there is any match
}
