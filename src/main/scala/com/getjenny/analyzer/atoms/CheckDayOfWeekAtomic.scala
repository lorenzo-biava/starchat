package com.getjenny.analyzer.atoms

/**
  * Created by angelo on 05/07/18.
  */

import com.getjenny.analyzer.expressions.{AnalyzersDataInternal, Result}
import com.getjenny.analyzer.util.{ComparisonOperators, Time}

/** Check if the current time is Equal, LessOrEqual, Less, Greater, GreaterOrEqual to the argument which
  * is an integer between 1 (MONDAY) and 7 (SUNDAY)
  *
  * first argument is the day of the week: a number between 1 and 7
  * second argument is the operator: any of Equal, LessOrEqual, Less, Greater, GreaterOrEqual
  * third argument is the timezone: UTC, GMT, UT, CET, UTC+<N>, UTC-<N>, GMT+<N>,
  *                     GMT-<N>, UT+<N> or UT-<N> where N is a number between -18 and +18. Default is CET
  */
class CheckDayOfWeekAtomic(val arguments: List[String],
                           restricted_args: Map[String, String]) extends AbstractAtomic {

  val argDayOfWeek: Long = arguments.headOption match {
    case Some(t) => t.toLong
    case _ => throw ExceptionAtomic("CheckDayOfWeekAtomic: must have at least one argument")
  }

  val argOperator: String = arguments.lift(1) match {
    case Some(t) => t
    case _ => "GreaterOrEqual"
  }

  val argZone: String = arguments.lift(2) match {
    case Some(t) => t
    case _ => "CET"
  }

  override def toString: String = "checkTime(\"" + argDayOfWeek + ", " + argOperator + "\")"
  val isEvaluateNormalized: Boolean = true
  def evaluate(query: String, data: AnalyzersDataInternal = AnalyzersDataInternal()): Result = {
    val weekDay = Time.dayOfWeekInt(argZone).toLong
    if(ComparisonOperators.compare(weekDay, argDayOfWeek, argOperator))
      Result(score = 1.0)
    else
      Result(score = 0.0)
  }
}

