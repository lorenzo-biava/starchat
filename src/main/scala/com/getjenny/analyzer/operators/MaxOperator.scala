package com.getjenny.analyzer.operators

import com.getjenny.analyzer.expressions._
import scalaz._
import Scalaz._

/**
  * Created by angelo on 21/06/17.
  */

class MaxOperator(children: List[Expression]) extends AbstractOperator(children: List[Expression]) {
  override def toString: String = "MaxOperator(" + children.mkString(", ") + ")"
  def add(e: Expression, level: Int = 0): AbstractOperator = {
    if (level === 0) new MaxOperator(e :: children)
    else {
      children.headOption match {
        case Some(t) =>
          t match {
            case c: AbstractOperator => new MaxOperator(c.add(e, level - 1) :: children.tail)
            case _ => throw OperatorException("MaxOperator: trying to add to smt else than an operator")
          }
        case _ =>
          throw OperatorException("MaxOperator: trying to add None instead of an operator")
      }
    }
  }

  def evaluate(query: String, data: AnalyzersDataInternal = new AnalyzersDataInternal): Result = {
    def compMax(l: List[Expression]): Result = {
      val val1 = l.head.evaluate(query, data)
      if (l.tail.isEmpty) {
        Result(score = val1.score,
          AnalyzersDataInternal(
            traversed_states = data.traversed_states,
            extracted_variables = val1.data.extracted_variables,
            data = val1.data.data
          )
        )
      } else {
        val val2 = compMax(l.tail)
        if(val1.score >= val2.score) val1 else val2
      }
    }
    compMax(children)
  }
}
