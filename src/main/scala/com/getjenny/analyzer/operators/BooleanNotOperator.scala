package com.getjenny.analyzer.operators

import com.getjenny.analyzer.expressions._

/**Not Operator
  *
  * It can only take one argument --we leave List so that Parser.gobble_command can add
  * one Expression, but throw exception if more than one child is added
  * Created by mal on 21/02/2017.
  */

class BooleanNotOperator(child: List[Expression]) extends AbstractOperator(child: List[Expression]) {
  require(child.length <= 1, "BooleanNotOperator can only have one Expression")
  override def toString: String = "booleanNot(" + child + ")"
  def add(e: Expression, level: Int = 0): AbstractOperator = {
    if (level == 0) {
      println("MINCHIAAAAAAAAAAAAA " + child + " e " + e + ". Empty? " + child.nonEmpty)
      if (child.nonEmpty) {
        println("OMMERDA........")
        throw OperatorException("BooleanNotOperator: trying to adding more than one expression.")
      }
      new BooleanNotOperator(e :: child)
    }
    else child.head match {
      case c: AbstractOperator => {
        println("CAZZOOOOOOOO " + child + " e " + e)
        if (child.tail.nonEmpty) throw OperatorException("BooleanNotOperator: more than one child expression.")
        new BooleanNotOperator(c.add(e, level - 1) :: child.tail)
      }
      case _ => throw OperatorException("BooleanNotOperator: trying to add to smt else than an operator.")
    }
  }
  def evaluate(query: String): Double = if(child.head.matches(query)) 0.0 else 1.0


}
