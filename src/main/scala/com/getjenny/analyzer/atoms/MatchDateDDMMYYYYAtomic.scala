package com.getjenny.analyzer.atoms

import com.getjenny.analyzer.expressions.Result
import com.getjenny.analyzer.utils._

/**
  * Created by angelo on 26/06/17.
  */

class MatchDateDDMMYYYYAtomic(val prefix: String) extends AbstractAtomic {
  override def toString: String = "matchDateDDMMYYYY(" + prefix + ")"
  val isEvaluateNormalized: Boolean = true
  val regex = """[""" + prefix + """day,""" + prefix + """month,""" + prefix + """year]""" +
    """(?:(0[1-9]|[12][0-9]|3[01])(?:[- \/\.])(0[1-9]|1[012])(?:[- \/\.])((?:19|20)\d\d))"""
  val regex_extractor = new PatternExtractionRegex(regex)

  def evaluate(query: String): Result = {
    val res = try {
      val extracted_variables = regex_extractor.evaluate(query)
      Result(score=1.0, extracted_variables = extracted_variables)
    } catch {
      case e: PatternExtractionNoMatchException =>
        Result(score=0)
      case e: Exception =>
        throw ExceptionAtomic("Parsing of regular expression specification(" + regex + "), query(" + query + ")", e)
    }
    res
  }
}