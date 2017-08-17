package com.getjenny.starchat.analyzer.atoms

import com.getjenny.analyzer.atoms.AbstractAtomic
import com.getjenny.analyzer.expressions.{Data, Result}

/**
  * Created by angelo on 16/08/17.
  */

/** test the value of the last traversed state
  *
  * @param name of the state to be checked
  */

class LastTravStateIsAtomic(val name: String) extends AbstractAtomic {
  override def toString: String = "lastTravStateIs"
  val isEvaluateNormalized: Boolean = true

  /** Check if the last state into data.item_list is <state>
    *
    * @param query the user query
    * @param data the data
    * @return Result with 1.0 if the last state is <name> score = 0.0 otherwise
    */
  def evaluate(query: String, data: Data = Data()): Result = {
    if(data.item_list.nonEmpty && data.item_list.last == name) {
      Result(score = 1.0)
    } else {
      Result(score = 0.0)
    }
  }
}
