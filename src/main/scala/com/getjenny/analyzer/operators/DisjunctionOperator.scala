package com.getjenny.analyzer.operators

import com.getjenny.analyzer.expressions._
import scalaz._
import Scalaz._

/**
  * Created by mal on 21/02/2017.
  */

class DisjunctionOperator(children: List[Expression]) extends AbstractOperator(children: List[Expression]) {
  override def toString: String = "disjunction(" + children.mkString(", ") + ")"
  def add(e: Expression, level: Int = 0): AbstractOperator = {
    if (level === 0) new DisjunctionOperator(e :: children)
    else children.head match {
      case c: AbstractOperator => new DisjunctionOperator(c.add(e, level - 1) :: children.tail)
      case _ => throw OperatorException("Disjunction: trying to add to smt else than an operator")
    }
  }

  def evaluate(query: String, data: AnalyzersData = new AnalyzersData): Result = {
    def compDisjunction(l: List[Expression]): Result = {
      if(l.isEmpty) {
        throw OperatorException("Disjuction argument list is empty")
      }
      val res = l.head.evaluate(query, data)
      if (l.tail.isEmpty) Result(score = 1.0 - res.score, data = res.data)
      else {
        val comp_disj = compDisjunction(l.tail)
        Result(score = (1.0 - res.score) * comp_disj.score,
          AnalyzersData(
            item_list = data.item_list,
            extracted_variables = comp_disj.data.extracted_variables ++ res.data.extracted_variables,
            data = comp_disj.data.data ++ res.data.data,
          )
        )
      }
    }
    val comp_disj = compDisjunction(children)
    Result(score=1.0 - comp_disj.score, data = comp_disj.data)
  }
}

