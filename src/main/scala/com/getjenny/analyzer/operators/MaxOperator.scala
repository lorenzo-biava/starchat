package com.getjenny.analyzer.operators

import com.getjenny.analyzer.expressions._

/**
  * Created by angelo on 21/06/17.
  */

class MaxOperator(children: List[Expression]) extends AbstractOperator(children: List[Expression]) {
  override def toString: String = "max(" + children.mkString(", ") + ")"
  def add(e: Expression, level: Int = 0): AbstractOperator = {
    if (level == 0) new MaxOperator(e :: children)
    else children.head match {
      case c: AbstractOperator => new MaxOperator(c.add(e, level - 1) :: children.tail)
      case _ => throw OperatorException("Max: trying to add to smt else than an operator")
    }
  }

  def evaluate(query: String, data: Data = new Data): Result = {
    def compMax(l: List[Expression]): Result = {
      val res = l.head.evaluate(query, data)
      if (l.tail == Nil) {
        Result(score = res.score,
          Data(
            item_list = data.item_list,
            extracted_variables = res.data.extracted_variables)
        )
      } else {
        val val1 = l.head.evaluate(query)
        val val2 = compMax(l.tail)
        if(val1.score >= val2.score) val1 else val2
      }
    }
    compMax(children)
  }
}
