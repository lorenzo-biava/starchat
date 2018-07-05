package com.getjenny.analyzer.atoms

/**
  * Created by angelo on 05/07/18.
  */

import com.getjenny.analyzer.expressions.{AnalyzersData, Result}
import com.getjenny.analyzer.util.{ComparisonOperators, Time}

/** Check if the current time is Equal, LessOrEqual, Less, Greater, GreaterOrEqual to the argument time in EPOC
  *
  * first argument is the hour: a number between 0 and 23
  * second argument is the operator: any of Equal, LessOrEqual, Less, Greater, GreaterOrEqual
  * third argument is the timezone: UTC, GMT, UT, CET, UTC+<N>, UTC-<N>, GMT+<N>,
  *                     GMT-<N>, UT+<N> or UT-<N> where N is a number between -18 and +18. Default is CET
  */
class CheckHourAtomic(val arguments: List[String],
                      restricted_args: Map[String, String]) extends AbstractAtomic {

  val argHour: Long = arguments.headOption match {
    case Some(t) => t.toLong
    case _ => throw ExceptionAtomic("CheckHourAtomic: must have at least one argument")
  }

  val argOperator: String = arguments.lift(1) match {
    case Some(t) => t
    case _ => "GreaterOrEqual"
  }

  val argZone: String = arguments.lift(2) match {
    case Some(t) => t
    case _ => "CET"
  }

  override def toString: String = "checkTime(\"" + argHour + ", " + argOperator + "\")"
  val isEvaluateNormalized: Boolean = true
  def evaluate(query: String, data: AnalyzersData = AnalyzersData()): Result = {
    val hour: Long = Time.hour(argZone)
    if(ComparisonOperators.compare(hour, argHour, argOperator))
      Result(score = 1.0)
    else
      Result(score = 0.0)
  }
}



