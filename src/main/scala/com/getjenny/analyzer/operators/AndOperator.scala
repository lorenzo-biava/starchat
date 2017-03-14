package com.getjenny.analyzer.operators

import com.getjenny.analyzer.expressions._

/**
  * Created by mal on 21/02/2017.
  */

class AndOperator(children: List[Expression]) extends AbstractOperator(children: List[Expression]) {
  override def toString: String = "boolean-and(" + children.mkString(", ") + ")"
  def add(e: Expression, level: Int = 0): AbstractOperator = {
    if (level == 0) new AndOperator(e :: children)
    else children.head match {
      case c: AbstractOperator => new AndOperator(c.add(e, level - 1) :: children.tail)
      case _ => throw OperatorException("boolean-and: trying to add to smt else than an operator")
    }
  }
  def evaluate(query: String): Double = {
    def loop(l: List[Expression]): Double = {
      if (!l.head.matches(query)) 0
      else if (l.tail == Nil) 1
      else loop(l.tail)
    }

    loop(children)
  }
}
