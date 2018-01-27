package com.getjenny.analyzer.operators

import com.getjenny.analyzer.expressions._
import scalaz._
import Scalaz._

/**
  * Created by mal on 21/02/2017.
  */

class DisjunctionOperator(children: List[Expression]) extends AbstractOperator(children: List[Expression]) {
  override def toString: String = "DisjunctionOperator(" + children.mkString(", ") + ")"
  def add(e: Expression, level: Int = 0): AbstractOperator = {
    if (level === 0) new DisjunctionOperator(e :: children)
    else {
      children.headOption match {
        case Some(t) =>
          t match {
            case c: AbstractOperator => new DisjunctionOperator(c.add(e, level - 1) :: children.tail)
            case _ => throw OperatorException("DisjunctionOperator: trying to add to smt else than an operator")
          }
        case _ =>
          throw OperatorException("DisjunctionOperator: trying to add None instead of an operator")
      }
    }
  }

  def evaluate(query: String, data: AnalyzersData = new AnalyzersData): Result = {
    def compDisjunction(l: List[Expression]): Result = {
      val res = l.headOption match {
        case Some(t) => {
          t.evaluate(query, data)
        }
        case _ =>
          throw OperatorException("DisjunctionOperator: operator argument is empty")
      }

      l.tailOption match {
        case Some(t) =>
          if (t.nonEmpty) {
            val comp_disj = compDisjunction(t)
            Result(score = (1.0 - res.score) * comp_disj.score,
              AnalyzersData(
                item_list = data.item_list,
                extracted_variables = comp_disj.data.extracted_variables ++ res.data.extracted_variables,
                data = comp_disj.data.data ++ res.data.data
              )
            )
          } else {
            Result(score = 1.0 - res.score, data = res.data)
          }
        case _ =>
          throw OperatorException("DisjunctionOperator: the tail must be a list")
      }
    }
    val comp_disj = compDisjunction(children)
    Result(score=1.0 - comp_disj.score, data = comp_disj.data)
  }
}

