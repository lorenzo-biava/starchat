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
  def evaluate(query: String, data: Data = Data()): Result = {
    def loop(l: List[Expression]): Result = {
      val res = l.head.matches(query, data)
      if (res.score == 1) Result(score=1, data = res.data)
      else if (l.tail == Nil) Result(score=0, data = res.data)
      else loop(l.tail)
    }
    loop(children)
  }
}

