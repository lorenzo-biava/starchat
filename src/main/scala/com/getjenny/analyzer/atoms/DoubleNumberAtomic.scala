package com.getjenny.analyzer.atoms

/**
  * Created by angelo on 19/10/18.
  */

import com.getjenny.analyzer.expressions.{AnalyzersDataInternal, Result}

/** Double Atomic
  *
  * argument is a double number
  */
class DoubleNumberAtomic(val arguments: List[String],
                      restricted_args: Map[String, String]) extends AbstractAtomic {

  val number: Double = arguments.headOption match {
    case Some(t) => t.toDouble
    case _ => throw ExceptionAtomic("DoubleNumberAtomic: must have one argument")
  }

  override def toString: String = "Double(\"" + number + "\")"
  val isEvaluateNormalized: Boolean = false
  def evaluate(query: String, data: AnalyzersDataInternal = AnalyzersDataInternal()): Result = {
    Result(score = number)
  }
}



