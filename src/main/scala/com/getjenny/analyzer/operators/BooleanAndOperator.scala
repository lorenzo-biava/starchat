package com.getjenny.analyzer.operators

import com.getjenny.analyzer.expressions._
import scalaz._
import Scalaz._

/**
  * Created by mal on 21/02/2017.
  */

class BooleanAndOperator(children: List[Expression]) extends AbstractOperator(children: List[Expression]) {
  override def toString: String = "BooleanAndOperator(" + children.mkString(", ") + ")"
  def add(e: Expression, level: Int = 0): AbstractOperator = {
    if (level === 0) new BooleanAndOperator(e :: children)
    else {
      children.headOption match {
        case Some(t) =>
          t match {
            case c: AbstractOperator => new BooleanAndOperator(c.add(e, level - 1) :: children.tail)
            case _ => throw OperatorException("BooleanAndOperator: trying to add to smt else than an operator")
          }
        case _ =>
          throw OperatorException("BooleanAndOperator: trying to add None instead of an operator")
      }
    }
  }

  def evaluate(query: String, data: AnalyzersDataInternal = AnalyzersDataInternal()): Result = {
    def loop(l: List[Expression]): Result = {
      val first_res = l.headOption match {
        case Some(t) => {
          t.matches(query, data)
        }
        case _ =>
          throw OperatorException("BooleanAndOperator: operator argument is empty")
      }
      if (first_res.score =/= 1.0d) {
        Result(score=0, data = first_res.data)
      }
      else if (l.tail.isEmpty) {
        Result(score=1, data = first_res.data)
      }
      else {
        val res = loop(l.tail)
        Result(score = res.score,
          AnalyzersDataInternal(
            traversed_states = data.traversed_states,
            extracted_variables = res.data.extracted_variables ++ first_res.data.extracted_variables,
            data = res.data.data ++ first_res.data.data
          )
        )
      }
    }

    loop(children)
  }
}
