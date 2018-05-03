package com.getjenny.analyzer.atoms

import com.getjenny.analyzer.expressions.{AnalyzersData, Result}

/**
  * Created by angelo on 26/06/17.
  */

/** test if a variable exists on dictionary of variables
  *
  * @param arguments name of the variable to be checked
  */
class ExistsVariableAtomic(val arguments: List[String], restricted_args: Map[String, String]) extends AbstractAtomic {
  val varName = arguments.headOption match {
    case Some(t) => t
    case _ => throw ExceptionAtomic("ExistsVariableAtomic: must have one argument")
  }

  override def toString: String = "existsVariable"
  val isEvaluateNormalized: Boolean = true

  /** Check if a variable named <varname> exists on the data variables dictionary
    *
    * @param query the user query
    * @param data the dictionary of variables
    * @return Result with 1.0 if the variable exists score = 0.0 otherwise
    */
  def evaluate(query: String, data: AnalyzersData = AnalyzersData()): Result = {
    if(data.extracted_variables.contains(varName)) {
      Result(score = 1.0)
    } else {
      Result(score = 0.0)
    }
  }
}