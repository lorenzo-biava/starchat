package com.getjenny.analyzer.operators

import com.getjenny.analyzer.expressions._

/**
  * Created by mal on 21/02/2017.
  */

class BooleanOrOperator(children: List[Expression]) extends AbstractOperator(children: List[Expression]) {
  override def toString: String = "booleanOr(" + children.mkString(", ") + ")"
  def add(e: Expression, level: Int): AbstractOperator = {
      if (level == 0) new BooleanOrOperator(e :: children)
      else children.head match {
        case c: AbstractOperator => new BooleanOrOperator(c.add(e, level - 1) :: children.tail)
        case _ => throw OperatorException("booleanOr: trying to add to smt else than an operator")
      }
  }
  def evaluate(query: String): Double = {
    def loop(l: List[Expression]): Double = {
      if (l.head.matches(query)) 1
      else if (l.tail == Nil) 0
      else loop(l.tail)
    }
    loop(children)
  }
}

