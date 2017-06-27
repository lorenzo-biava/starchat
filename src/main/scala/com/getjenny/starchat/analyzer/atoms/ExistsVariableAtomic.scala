package com.getjenny.starchat.analyzer.atoms

import com.getjenny.analyzer.expressions.Result
import com.getjenny.analyzer.atoms.AbstractAtomic
import com.getjenny.analyzer.atoms.ExceptionAtomic
import com.getjenny.starchat.analyzer.utils._

/**
  * Created by angelo on 27/06/17.
  */

class ExistsVariableAtomic(val name: String) extends AbstractAtomic {
  override def toString: String = "existsVariable(" + name + ")"
  val isEvaluateNormalized: Boolean = true

  def evaluate(query: String): Result = {

    //TODO: WIP
    val res = Result(score=1.0)
    res
  }
}