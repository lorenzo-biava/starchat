package com.getjenny.analyzer.atoms

import com.getjenny.analyzer.expressions.{AnalyzersData, Result}
import com.getjenny.analyzer.utils._

/**
  * Created by angelo on 27/06/17.
  */

/** A generic pattern extraction analyzer, it extract named patterns matching a given regex
  *   e.g. the following will match tree numbers separated by semicolumn:
  *     [first,second,third](?:([0-9]+:[0-9]:[0-9]+)
  *   if the regex matches it will create the entries into the dictionary e.g.:
  *     10:11:12 will result in Map("first.0" -> "10", "second.0" -> "11", "third.0" -> "12")
  *     the number at the end of the name is an index incremented for multiple occurrences of the pattern
  *     in the query
  *
  * @param arguments the regular expression in the form [<name0>,..,<nameN>](<regex>)
  */
class MatchPatternRegexAtomic(val arguments: List[String], restricted_args: Map[String, String]) extends AbstractAtomic {
  val regex = arguments(0)
  override def toString: String = "matchPatternRegex(" + regex + ")"
  val isEvaluateNormalized: Boolean = true

  val regexExtractor = new PatternExtractionRegex(regex)

  def evaluate(query: String, data: AnalyzersData = AnalyzersData()): Result = {
    val res = try {
      Result(
        score = 1.0,
        AnalyzersData(item_list = data.item_list, extracted_variables = regexExtractor.evaluate(query))
      )
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
