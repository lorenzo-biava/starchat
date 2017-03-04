package com.getjenny.starchat.analyzer.operators

import com.getjenny.starchat.analyzer.expressions._

/**
  * Created by mal on 21/02/2017.
  */
abstract class AbstractOperator(children: List[Expression]) extends Expression {
  def add(e: Expression, level: Int): AbstractOperator
  def head: Expression = children.head
  def tail: List[Expression] = children.tail
}

