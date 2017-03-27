package com.getjenny.analyzer.operators

import com.getjenny.analyzer.expressions._

/** Operators are built to be nested through the add method.
  *
  * For instance the operator:
  *
  * operator1(operator2(atomic1("..."), atomic2("...")), operator3(atomic3("...")))
  *
  * The parser would build the final operator with:
  *
  * disjunction.add(operator1, 0)
  *               .add(operator2, 1)
  *                   .add(atomic1, 2)
  *                   .add(atomic2, 2)
  *               .add(operator3, 1)
  *                   .add(atomic3, 2)
  *
  * (Where disjunction is put as starting point because disjunction(one_expression) == one_expression)
  *
  * This works because add always add the new Expression as head, and when we always add on the head.
  * When add(atomic1, 2) is called, atomic1 is added as head of the children in operator2,
  * which was just added as head of the children of op1, which was added as the head of disjunction.
  *
  * Created by mario@getjenny.com on 21/02/2017.
  *
  */
abstract class AbstractOperator(children: List[Expression]) extends Expression {
  /** Possible implementation:
    ```scala
    def add(e: Expression, level: Int = 0): AbstractOperator = {
      if (level == 0) new AndOperator(e :: children)
      else children.head match {
        case c: AbstractOperator => new XYZOperator(c.add(e, level - 1) :: children.tail)
        case _ => throw OperatorException(this + ": trying to add to smt else than an operator")
      }
    }
   ```
    *
    *
    * @param e: expression to add (e.g. Operator or Atomic)
    * @param level: how many level down the operator's tree
    * @return
    */
    def add(e: Expression, level: Int): AbstractOperator

  def head: Expression = children.head
  def tail: List[Expression] = children.tail
}

