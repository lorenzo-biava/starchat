package com.getjenny.analyzer.atoms

/**
  * Created by angelo on 05/07/18.
  */

import com.getjenny.analyzer.expressions.{AnalyzersData, Result}
import com.getjenny.analyzer.util.{ComparisonOperators, Time}

/** Check if the current time is Equal, LessOrEqual, Less, Greater, GreaterOrEqual to the argument time in EPOC
  *
  * first argument is the timestamp: a timestamp in EPOC (in seconds)
  * second argument is the operator: any of Equal, LessOrEqual, Less, Greater, GreaterOrEqual
  */
class CheckTimestampAtomic(val arguments: List[String],
                           restricted_args: Map[String, String]) extends AbstractAtomic {

  val argTimestamp: Long = arguments.headOption match {
    case Some(t) => t.toLong
    case _ => throw ExceptionAtomic("CheckTimestampAtomic: must have at least one argument")
  }

  val argOperator: String = arguments.lastOption match {
    case Some(t) => t
    case _ => "GreaterOrEqual"
  }

  override def toString: String = "checkTime(\"" + argTimestamp + ", " + argOperator + "\")"
  val isEvaluateNormalized: Boolean = true
  def evaluate(query: String, data: AnalyzersData = AnalyzersData()): Result = {
    val currTimestamp: Long = Time.timestampEpoc
    if(ComparisonOperators.compare(currTimestamp, argTimestamp, argOperator))
      Result(score = 1.0)
    else
      Result(score = 0.0)
  }
}
