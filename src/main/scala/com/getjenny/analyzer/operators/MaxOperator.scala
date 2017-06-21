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
  def evaluate(query: String): Double = {
    def compMax(l: List[Expression]): Double = {
      if (l.tail == Nil) l.head.evaluate(query)
      else math.max(l.head.evaluate(query), compMax(l.tail))
    }
    compMax(children)
  }
}
