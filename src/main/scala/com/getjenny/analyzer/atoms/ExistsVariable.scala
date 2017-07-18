package com.getjenny.analyzer.atoms

import com.getjenny.analyzer.expressions.Result
import com.getjenny.analyzer.utils._

/**
  * Created by angelo on 26/06/17.
  */

class ExistsVariable(val varname: String) extends AbstractAtomic {
  override def toString: String = "existsVariable"
  val isEvaluateNormalized: Boolean = true
  def evaluate(query: String, data: Option[Map[String, String]] = None): Result = {
    if(data.isDefined && data.get.exists(_._1 == varname)) {
      Result(score = 1.0)
    } else {
      Result(score = 0.0)
    }
  }
}