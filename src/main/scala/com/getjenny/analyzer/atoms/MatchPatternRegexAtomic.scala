package com.getjenny.analyzer.atoms

import com.getjenny.analyzer.expressions.Result
import com.getjenny.analyzer.utils._

/**
  * Created by angelo on 27/06/17.
  */

class MatchPatternRegexAtomic(val regex: String) extends AbstractAtomic {
  override def toString: String = "matchPatternRegex(" + regex + ")"
  val isEvaluateNormalized: Boolean = true

  val regex_extractor = new PatternExtractionRegex(regex)

  def evaluate(query: String): Result = {
    val res = try {
      val extracted_variables = regex_extractor.evaluate(query)
      Result(score=1.0, extracted_variables=extracted_variables)
    } catch {
      case e: PatternExtractionNoMatchException =>
        //println("DEBUG: no match for regular expression specification(" + regex + "), query(" + query + ")")
        Result(score=0)
      case e: Exception =>
        throw ExceptionAtomic("Parsing of regular expression specification(" + regex + "), query(" + query + ")", e)
    }
    res
  }
}
