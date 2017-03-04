package com.getjenny.starchat.analyzer

/**
  * Created by mal on 21/02/2017.
  */
abstract class Operator(children: List[Expression])  extends Expression {
  def add(e: Expression, level: Int): Operator
  def head: Expression = children.head
  def tail: List[Expression] = children.tail
}

class OR(children: List[Expression]) extends Operator(children: List[Expression]) {
  override def toString: String = "or(" + children.mkString(", ") + ")"
  def add(e: Expression, level: Int): Operator = {
      if (level == 0) new OR(e :: children)
      else children.head match {
        case c: Operator => new OR(c.add(e, level - 1) :: children.tail)
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

class AND(children: List[Expression]) extends Operator(children: List[Expression]) {
  override def toString: String = "and(" + children.mkString(", ") + ")"
  def add(e: Expression, level: Int = 0): Operator = {
    if (level == 0) new AND(e :: children)
    else children.head match {
      case c: Operator => new AND(c.add(e, level - 1) :: children.tail)
      case _ => throw new Exception("AND: trying to add to smt else than an operator")
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

class Disjunction(children: List[Expression]) extends Operator(children: List[Expression]) {
  override def toString: String = "disjunction(" + children.mkString(", ") + ")"
  def add(e: Expression, level: Int = 0): Operator = {
    if (level == 0) new Disjunction(e :: children)
    else children.head match {
      case c: Operator => new Disjunction(c.add(e, level - 1) :: children.tail)
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

class Conjunction(children: List[Expression]) extends Operator(children: List[Expression]) {
  override def toString: String = "conjunction(" + children.mkString(", ") + ")"
  def add(e: Expression, level: Int = 0): Operator = {
    if (level == 0) new Conjunction(e :: children)
    else children.head match {
      case c: Operator => new Conjunction(c.add(e, level - 1) :: children.tail)
      case _ => throw new Exception("Conjunction: trying to add to smt else than an operator")
    }
  }
  def evaluate(query: String): Double = {
    def conjunction(l: List[Expression]): Double = {
      if (l.head.evaluate(query) == 0) 0
      else if (l.tail == Nil) l.head.evaluate(query)
      else l.head.evaluate(query) * conjunction(l.tail)
    }
    conjunction(children)
  }
}
