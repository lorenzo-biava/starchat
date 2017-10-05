package com.getjenny.starchat.analyzer.atoms

import com.getjenny.analyzer.atoms.AbstractAtomic
import com.getjenny.analyzer.expressions.{AnalyzersData, Result}

/**
  * Created by angelo on 16/08/17.
  */

/** test the value of the penultimate traversed state
  *
  * @param arguments of the state to be checked
  */


class PreviousTravStateIsAtomic(val arguments: List[String]) extends AbstractAtomic {
  val name = arguments(0)
  override def toString: String = "prevTravStateIs"
  val isEvaluateNormalized: Boolean = true

  /** Check if the last state into data.item_list is <state>
    *
    * @param query the user query
    * @param data the data
    * @return Result with 1.0 if the penultimate state is <name> score = 0.0 otherwise
    */
  def evaluate(query: String, data: AnalyzersData = AnalyzersData()): Result = {
    val list_length = data.item_list.length
    if(list_length >= 2 && data.item_list(list_length-2) == name) {
      Result(score = 1.0)
    } else {
      Result(score = 0.0)
    }
  }
}