package com.getjenny.analyzer.operators

import com.getjenny.analyzer.expressions._
import com.getjenny.analyzer.analyzers._
import scalaz._
import Scalaz._

/**
  * Created by mal on 21/02/2017.
  */

class ConjunctionOperator(children: List[Expression]) extends AbstractOperator(children: List[Expression]) {
  override def toString: String = "ConjunctionOperator(" + children.mkString(", ") + ")"
  def add(e: Expression, level: Int = 0): AbstractOperator = {
    if (level === 0) {
      new ConjunctionOperator(e :: children)
    } else if(children.isEmpty){
      throw OperatorException("ConjunctionOperator: children list is empty")
    } else {
      children.headOption match {
        case Some(t) =>
          t match {
            case c: AbstractOperator => new ConjunctionOperator(c.add(e, level - 1) :: children.tail)
            case _ => throw OperatorException("ConjunctionOperator: trying to add to smt else than an operator")
          }
        case _ => throw OperatorException("ConjunctionOperator: trying to add None instead of an operator")

      }
    }
  }

  def evaluate(query: String, data: AnalyzersData = AnalyzersData()): Result = {
    def conjunction(l: List[Expression]): Result = {
      val eval = l.headOption match {
        case Some(t) =>
          t.evaluate(query, data)
        case _ =>
          throw OperatorException("ConjunctionOperator: argument list is empty")
      }
      if (eval.score == 0) Result(score = 0, data = eval.data)
      else if (l.tail.isEmpty) eval
      else {
        val res = conjunction(l.tail)

        Result(score = eval.score * res.score,
          AnalyzersData(
            item_list = data.item_list,
            extracted_variables = eval.data.extracted_variables ++ res.data.extracted_variables,
            data = eval.data.data ++ res.data.data
          )
        )
      }
    }
    conjunction(children)
  }
}
