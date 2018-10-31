package com.getjenny.starchat.analyzer.atoms

import com.getjenny.analyzer.atoms.{AbstractAtomic, ExceptionAtomic}
import com.getjenny.analyzer.expressions.{AnalyzersDataInternal, Result}

/**
  * Created by angelo on 16/08/17.
  */

/** test if the list of traversed states contains a state, eg
  *
  * hasTravState("help")
  *
  * @param arguments of the state to be checked
  */

class HasTravStateAtomic(val arguments: List[String], restrictedArgs: Map[String, String]) extends AbstractAtomic {
  val name: String = arguments.headOption match {
    case Some(t) => t
    case _ =>
      throw ExceptionAtomic("hasTravState requires an argument")
  }

  override def toString: String = "hasTravState"
  val isEvaluateNormalized: Boolean = true

  /** Check if the state <name> exists on the list of traversed states data.traversed_states
    *
    * @param query the user query
    * @param data the data
    * @return Result with 1.0 if the state exists score = 0.0 otherwise
    */
  def evaluate(query: String, data: AnalyzersDataInternal = AnalyzersDataInternal()): Result = {
    if(data.traversed_states.contains(name)) {
      Result(score = 1.0)
    } else {
      Result(score = 0.0)
    }
  }
}
