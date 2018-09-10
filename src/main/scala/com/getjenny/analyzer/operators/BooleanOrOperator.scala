package com.getjenny.analyzer.operators

import com.getjenny.analyzer.expressions._
import scalaz.Scalaz._

/**
  * Created by mal on 21/02/2017.
  */

class BooleanOrOperator(children: List[Expression]) extends AbstractOperator(children: List[Expression]) {
  override def toString: String = "BooleanOrOperator(" + children.mkString(", ") + ")"
  def add(e: Expression, level: Int): AbstractOperator = {

    if (level === 0) new BooleanOrOperator(e :: children)
    else {
      children.headOption match {
        case Some(t) =>
          t match {
            case c: AbstractOperator => new BooleanOrOperator(c.add(e, level - 1) :: children.tail)
            case _ => throw OperatorException("BooleanOrOperator: trying to add to smt else than an operator")
          }
        case _ =>
          throw OperatorException("BooleanOrOperator: trying to add None instead of an operator")
      }
    }
  }

  def evaluate(query: String, data: AnalyzersDataInternal = AnalyzersDataInternal()): Result = {
    def loop(l: List[Expression]): Result = {
      val res = l.headOption match {
        case Some(arg) => arg.matches(query, data)
        case _ => throw OperatorException("BooleanOrOperator: inner expression is empty")
      }
      if (res.score === 1) Result(score=1, data = res.data)
      else if (l.tail.isEmpty) Result(score=0, data = res.data)
      else loop(l.tail)
    }
    loop(children)
  }
}

