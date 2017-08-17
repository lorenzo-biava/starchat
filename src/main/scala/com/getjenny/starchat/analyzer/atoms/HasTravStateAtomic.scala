package com.getjenny.starchat.analyzer.atoms

import com.getjenny.analyzer.atoms.AbstractAtomic
import com.getjenny.analyzer.expressions.{Data, Result}

/**
  * Created by angelo on 16/08/17.
  */

/** test if the list of traversed states contains a state
  *
  * @param name of the state to be checked
  */

class HasTravStateAtomic(val name: String) extends AbstractAtomic {
  override def toString: String = "hasTravState"
  val isEvaluateNormalized: Boolean = true

  /** Check if the state <name> exists on the list of traversed states data.item_list
    *
    * @param query the user query
    * @param data the data
    * @return Result with 1.0 if the state exists score = 0.0 otherwise
    */
  def evaluate(query: String, data: Data = Data()): Result = {
    if(data.item_list.contains(name)) {
      Result(score = 1.0)
    } else {
      Result(score = 0.0)
    }
  }
}
