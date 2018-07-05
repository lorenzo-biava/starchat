package com.getjenny.analyzer.atoms

/**
  * Created by angelo on 05/07/18.
  */

import com.getjenny.analyzer.expressions.{AnalyzersData, Result}
import com.getjenny.analyzer.util.{ComparisonOperators, Time}

/** Check if the current minutes are Equal, LessOrEqual, Less, Greater, GreaterOrEqual to the first argument
  *
  * first argument is the minute: a number between 0 and 59
  * second argument is the operator: any of Equal, LessOrEqual, Less, Greater, GreaterOrEqual
  * third argument is the timezone: UTC, GMT, UT, CET, UTC+<N>, UTC-<N>, GMT+<N>,
  *                     GMT-<N>, UT+<N> or UT-<N> where N is a number between -18 and +18. Default is CET
  */
class CheckMinuteAtomic(val arguments: List[String],
                      restricted_args: Map[String, String]) extends AbstractAtomic {

  val argMinute: Long = arguments.headOption match {
    case Some(t) => t.toLong
    case _ => throw ExceptionAtomic("CheckMinuteAtomic: must have at least one argument")
  }

  val argOperator: String = arguments.lift(1) match {
    case Some(t) => t
    case _ => "GreaterOrEqual"
  }

  val argZone: String = arguments.lift(2) match {
    case Some(t) => t
    case _ => "CET"
  }

  override def toString: String = "checkTime(\"" + argMinute + ", " + argOperator + "\")"
  val isEvaluateNormalized: Boolean = true
  def evaluate(query: String, data: AnalyzersData = AnalyzersData()): Result = {
    val minute: Long = Time.minutes(argZone)
    if(ComparisonOperators.compare(minute, argMinute, argOperator))
      Result(score = 1.0)
    else
      Result(score = 0.0)
  }
}



