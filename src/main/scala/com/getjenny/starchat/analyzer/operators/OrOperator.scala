package com.getjenny.starchat.analyzer.operators

import com.getjenny.starchat.analyzer.expressions._

/**
  * Created by mal on 21/02/2017.
  */

class OrOperator(children: List[Expression]) extends AbstractOperator(children: List[Expression]) {
  override def toString: String = "or(" + children.mkString(", ") + ")"
  def add(e: Expression, level: Int): AbstractOperator = {
      if (level == 0) new OrOperator(e :: children)
      else children.head match {
        case c: AbstractOperator => new OrOperator(c.add(e, level - 1) :: children.tail)
        case _ => throw new Exception("OR: trying to add to smt else than an operator")
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

