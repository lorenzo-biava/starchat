package com.getjenny.analyzer.atoms

import com.getjenny.analyzer.expressions.{AnalyzersData, Result}
import com.getjenny.analyzer.util._

import scala.util.Try
import scala.util.control.NonFatal

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
  val regex = arguments.headOption match {
    case Some(t) => t
    case _ => throw ExceptionAtomic("MatchPatternRegexAtomic: must have one argument")
  }
  override def toString: String = "matchPatternRegex(" + regex + ")"
  val isEvaluateNormalized: Boolean = true

  val regexExtractor = new PatternExtractionRegex(regex)

  def evaluate(query: String, data: AnalyzersData = AnalyzersData()): Result = {
    val res = Try(Result(score = 1.0,
      AnalyzersData(item_list = data.item_list, extracted_variables = regexExtractor.evaluate(query))
      )) recover {
      case e: PatternExtractionNoMatchException =>
        //println("DEBUG: no match for regular expression specification(" + regex + "), query(" + query + ")")
        Result(score=0)
      case NonFatal(e) =>
        throw ExceptionAtomic("Parsing of regular expression specification(" + regex + "), query(" + query + ")", e)
    }
    res.get
  }
}
