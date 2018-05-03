package com.getjenny.analyzer.expressions

/**
  * This the basic structure of StarChat Domain Specific Language.
  *
  * Used for AbstractAtomic or AbstractOperator
  *
  * Created by mal on 20/02/2017.
  */

abstract class Expression {
  /**
    * @param query see AbstractAtomic or AbstractOperator
    * @param data the Map exchanged between StarChat and the other services
    * @return a score (>= 0) which represents its confidence on triggering the state it is in
    */
  def evaluate(query: String, data: AnalyzersData = AnalyzersData()): Result // read a sentence and produce a score (the higher, the more confident)

  /**
    * In case of boolean logic, the threshold above which it says the state should be triggered
    */
  val match_threshold = 0.0

  /**
    *
    * @param query see AbstractAtomic or AbstractOperator
    * @param data the Map exchanged between StarChat and the other services
    * @return Should the state be triggered true/false
    */
  def matches(query: String, data: AnalyzersData = AnalyzersData()): Result = {
    val res = this.evaluate(query, data)
    val bool = if(res.score > match_threshold) 1.0 else 0.0
    if (bool == 1) println("DEBUG: Expression: " + this + " matches " + query)
    Result(score = bool, data = res.data)
  } // read a sentence and tells if there is any match
}
