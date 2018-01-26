package com.getjenny.analyzer.operators

import com.getjenny.analyzer.expressions._
import com.getjenny.analyzer.analyzers._

/**
  * Created by mal on 21/02/2017.
  */

class ConjunctionOperator(children: List[Expression]) extends AbstractOperator(children: List[Expression]) {
  override def toString: String = "conjunction(" + children.mkString(", ") + ")"
  def add(e: Expression, level: Int = 0): AbstractOperator = {
    if (level == 0) {
      new ConjunctionOperator(e :: children)
    } else if(children.isEmpty){
      throw OperatorException("Conjunction children list is empty")
    } else {
      children.headOption match {
        case c: Option[AbstractOperator] => new ConjunctionOperator(c.get.add(e, level - 1) :: children.tail)
        case _ => throw OperatorException("Conjunction: trying to add to smt else than an operator")
      }
    }
  }

  def evaluate(query: String, data: AnalyzersData = AnalyzersData()): Result = {
    def conjunction(l: List[Expression]): Result = {
      if(l.isEmpty) {
        throw OperatorException("Conjunction argument list is empty")
      }
      val eval = l.head.evaluate(query, data)
      if (eval.score == 0) Result(score = 0, data = eval.data)
      else if (l.tail == Nil) eval
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
