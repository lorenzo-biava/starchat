package com.getjenny.analyzer.atoms

/**
  * Created by angelo on 05/07/18.
  */

import com.getjenny.analyzer.expressions.{AnalyzersData, Result}
import com.getjenny.analyzer.util.{ComparisonOperators, Time}

/** Check if the current month is Equal, LessOrEqual, Less, Greater, GreaterOrEqual to the argument which
  * is an integer between 1 (JANUARY) and 12 (DECEMBER)
  *
  * first argument is the month's number: an integer between 1 and 12
  * second argument is the operator: any of Equal, LessOrEqual, Less, Greater, GreaterOrEqual
  * third argument is the timezone: UTC, GMT, UT, CET, UTC+<N>, UTC-<N>, GMT+<N>,
  *                     GMT-<N>, UT+<N> or UT-<N> where N is a number between -18 and +18. Default is CET
  */
class CheckMonthAtomic(val arguments: List[String],
                       restricted_args: Map[String, String]) extends AbstractAtomic {

  val argMonth: Long = arguments.headOption match {
    case Some(t) => t.toLong
    case _ => throw ExceptionAtomic("CheckMonthAtomic: must have at least one argument")
  }

  val argOperator: String = arguments.lift(1) match {
    case Some(t) => t
    case _ => "GreaterOrEqual"
  }

  val argZone: String = arguments.lift(2) match {
    case Some(t) => t
    case _ => "CET"
  }

  override def toString: String = "checkTime(\"" + argMonth + ", " + argOperator + "\")"
  val isEvaluateNormalized: Boolean = true
  def evaluate(query: String, data: AnalyzersData = AnalyzersData()): Result = {
    val month: Long = Time.monthInt(argZone).toLong
    if(ComparisonOperators.compare(month, argMonth, argOperator))
      Result(score = 1.0)
    else
      Result(score = 0.0)
  }
}


