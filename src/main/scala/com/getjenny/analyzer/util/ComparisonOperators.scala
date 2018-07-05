package com.getjenny.analyzer.util

/**
  * Created by Angelo on 05/07/2018.
  */

import com.getjenny.analyzer.atoms.ExceptionAtomic

object ComparisonOperators {
  def compare(x: Long, y: Long, operator: String): Boolean = operator match {
    case "LessOrEqual" => x <= y
    case "Less" => x < y
    case "Greater" => x > y
    case "GreaterOrEqual" => x >= y
    case "Equal" => x == y
    case _ => throw ExceptionAtomic("Bad Operator: " +
      "the operator must be: Equal, LessOrEqual, Less, Greater or GreaterOrEqual")
  }
}
