package com.getjenny.analyzer.atoms

import com.getjenny.analyzer.expressions.{Data, Result}

/**
  * Created by mal on 20/02/2017.
  */

/** Counts the occurrences of a pattern into a string
  *
  * @param arguments regular expression
  */
class RegularExpressionAtomic(arguments: List[String]) extends AbstractAtomic {
  /**
    * A Regex
    */
  val re = arguments(0)
  override def toString: String = "regex(\"" + re + "\")"
  val isEvaluateNormalized: Boolean = false
  private val rx = re.r

  def evaluate(query: String, data: Data = Data()): Result = {
    val score = rx.findAllIn(query).toList.length
    Result(score = score)
  }
}
