package com.getjenny.analyzer.operators

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 03/03/17.
  */

import com.getjenny.analyzer.expressions.Expression
import com.getjenny.analyzer.interfaces._

class StarchatFactoryOperator extends Factory[List[Expression], AbstractOperator] {

  override val operations = Set("or" , "and", "conjunction", "disjunction")

  override def get(name: String, argument: List[Expression]): AbstractOperator = name.filter(c => !c.isWhitespace ) match {
    case "or" => new OrOperator(argument)
    case "and" => new AndOperator(argument)
    case "conjunction" => new ConjunctionOperator(argument)
    case "disjunction" => new DisjunctionOperator(argument)
    case _ => throw new OperatorNotFoundException("Operator \'" + name + "\' not found")
  }

}