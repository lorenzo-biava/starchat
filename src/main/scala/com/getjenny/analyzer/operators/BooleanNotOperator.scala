package com.getjenny.analyzer.operators

import com.getjenny.analyzer.expressions._

/** Not Operator
  *
  * It can only take one argument --we leave List instead of Set as argument
  * so that Parser.gobble_command can add one Expression. Still, we throw exception if
  * more than one child is added
  *
  * Created by mal on 21/02/2017.
  */

class BooleanNotOperator(child: List[Expression]) extends AbstractOperator(child: List[Expression]) {
  require(child.length <= 1, "BooleanNotOperator can only have one Expression")
  override def toString: String = "booleanNot(" + child + ")"
  def add(e: Expression, level: Int = 0): AbstractOperator = {
    if (level == 0) {
      if (child.nonEmpty)
        throw OperatorException("BooleanNotOperator: trying to add more than one expression.")
      new BooleanNotOperator(e :: child)
    }
    else child.head match {
      case c: AbstractOperator =>
        if (child.tail.nonEmpty) throw OperatorException("BooleanNotOperator: more than one child expression.")
        new BooleanNotOperator(c.add(e, level - 1) :: child.tail)
      case _ => throw OperatorException("BooleanNotOperator: trying to add to smt else than an operator.")
    }
  }

  def evaluate(query: String, data: Data = Data()): Result = {
    val res = child.head.matches(query, data)
    Result(score=1 - res.score, data = res.data)
  }
}
