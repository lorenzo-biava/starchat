package com.getjenny.analyzer.expressions

/**
  * This the basic structure of StarChat Domain Specific Language.
  *
  *
  *
  * Created by mal on 20/02/2017.
  */

abstract class Expression {
  def evaluate(query: String, data: AnalyzersData = AnalyzersData()): Result // read a sentence and produce a score (the higher, the more confident)
  val matchThreshold = 0.0
  def matches(query: String, data: AnalyzersData = AnalyzersData()): Result = {
    val res = this.evaluate(query, data)
    val bool = if(res.score > matchThreshold) 1.0 else 0.0
    if (bool == 1) println("DEBUG: Expression: " + this + " matches " + query)
    Result(score = bool, data = res.data)
  } // read a sentence and tells if there is any match
}
