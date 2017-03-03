package com.getjenny.starchat.analyzer.operators

import com.getjenny.starchat.analyzer.expressions._

/**
  * Created by mal on 21/02/2017.
  */

class DisjunctionOperator(children: List[Expression]) extends AbstractOperator(children: List[Expression]) {
  override def toString: String = "disjunction(" + children.mkString(", ") + ")"
  def add(e: Expression, level: Int = 0): AbstractOperator = {
    if (level == 0) new DisjunctionOperator(e :: children)
    else children.head match {
      case c: AbstractOperator => new DisjunctionOperator(c.add(e, level - 1) :: children.tail)
      case _ => throw new Exception("Disjunction: trying to add to smt else than an operator")
    }
  }
  def evaluate(query: String): Double = {
    def compDisjunction(l: List[Expression]): Double = {
      if (l.head.evaluate(query) == 1) 1
      else if (l.tail == Nil) 1.0 - l.head.evaluate(query)
      else (1.0 - l.head.evaluate(query)) * compDisjunction(l.tail)
    }
    1.0 - compDisjunction(children)
  }
}

