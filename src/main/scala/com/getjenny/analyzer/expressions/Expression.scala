package com.getjenny.analyzer.expressions

/**
  * Created by mal on 20/02/2017.
  */

case class Result(score: Double, extracted_variables: Map[String, String] = Map.empty[String, String])

abstract class Expression {
  def evaluate(query: String): Result // read a sentence and produce a score (the higher, the more confident)
  val match_threshold = 0.0
  def matches(query: String): Result = {
    val res = this.evaluate(query)
    val bool = if(res.score > match_threshold) 1.0 else 0.0
    if (bool == 1) println("DEBUG: Expression: " + this + " matches " + query)
    Result(score = bool, extracted_variables = res.extracted_variables)
  } // read a sentence and tells if there is any match
}
