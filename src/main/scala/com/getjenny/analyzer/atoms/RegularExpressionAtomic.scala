package com.getjenny.analyzer.atoms

import com.getjenny.analyzer.expressions.{Data, Result}

/**
  * Created by mal on 20/02/2017.
  */

/** Counts the occurrences of a pattern into a string
  *
  * @param re regular expression
  */
class RegularExpressionAtomic(re: String) extends AbstractAtomic {
  /**
    * A Regex
    */
  override def toString: String = "regex(\"" + re + "\")"
  val isEvaluateNormalized: Boolean = false
  private val rx = re.r

  def evaluate(query: String, data: Data = Data()): Result = {
    val score = rx.findAllIn(query).toList.length
    Result(score = score)
  }
}
