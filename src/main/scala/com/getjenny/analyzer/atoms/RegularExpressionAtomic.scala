package com.getjenny.analyzer.atoms

import com.getjenny.analyzer.expressions.Result

/**
  * Created by mal on 20/02/2017.
  */

class RegularExpressionAtomic(re: String) extends AbstractAtomic {
  /**
    * A Regex
    */
  override def toString: String = "regex(\"" + re + "\")"
  val isEvaluateNormalized: Boolean = false
  private val rx = re.r
  def evaluate(query: String): Result = {
    val score = rx.findAllIn(query).toList.length
    Result(score = score)
  }
}
