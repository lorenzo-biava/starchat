package com.getjenny.analyzer.operators

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 03/03/17.
  */

import com.getjenny.analyzer.expressions.Expression
import com.getjenny.analyzer.interfaces._

class DefaultFactoryOperator extends Factory[List[Expression], AbstractOperator] {

  override val operations = Set("or" , "and", "conjunction", "disjunction", "bor", "band", "booleanor", "booleanand",
    "booleanOr", "booleanAnd", "booleanNot", "booleannot", "bnot", "maximum", "max")

  override def get(name: String, argument: List[Expression]): AbstractOperator = name.filter(c => !c.isWhitespace ) match {
    case ("booleanOr" | "booleanor" | "bor") => new BooleanOrOperator(argument)
    case ("booleanAnd"| "booleanand"| "band") => new BooleanAndOperator(argument)
    case ("booleanNot"| "booleannot"| "bnot") => new BooleanNotOperator(argument)
    case ("conjunction" | "and") => new ConjunctionOperator(argument)
    case ("disjunction" | "or") => new DisjunctionOperator(argument)
    case ("maximum" | "max") => new MaxOperator(argument)
    case _ => throw OperatorNotFoundException("Operator \'" + name + "\' not found")
  }

}