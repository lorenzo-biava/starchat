package com.getjenny.analyzer.expressions

/**
  * Created by mal on 20/02/2017.
  */

abstract class Expression {
  def evaluate(query: String, data: Data = Data()): Result // read a sentence and produce a score (the higher, the more confident)
  val match_threshold = 0.0
  def matches(query: String, data: Data = Data()): Result = {
    val res = this.evaluate(query, data)
    val bool = if(res.score > match_threshold) 1.0 else 0.0
    if (bool == 1) println("DEBUG: Expression: " + this + " matches " + query)
    Result(score = bool, data = res.data)
  } // read a sentence and tells if there is any match
}
