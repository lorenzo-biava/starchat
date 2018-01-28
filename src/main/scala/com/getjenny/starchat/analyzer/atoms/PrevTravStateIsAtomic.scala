package com.getjenny.starchat.analyzer.atoms

import com.getjenny.analyzer.atoms.{AbstractAtomic,ExceptionAtomic}
import com.getjenny.analyzer.expressions.{AnalyzersData, Result}
import scalaz.Scalaz._

/**
  * Created by angelo on 16/08/17.
  */

/** test the value of the penultimate traversed state
  *
  * @param arguments of the state to be checked
  */


class PreviousTravStateIsAtomic(val arguments: List[String], restricted_args: Map[String, String])
  extends AbstractAtomic {
  val name: String = arguments.headOption match {
    case Some(t) => t
    case _ =>
      throw ExceptionAtomic("prevTravStateIs requires an argument")
  }
  override def toString: String = "prevTravStateIs"
  val isEvaluateNormalized: Boolean = true

  /** Check if the last state into data.item_list is <state>
    *
    * @param query the user query
    * @param data the data
    * @return Result with 1.0 if the penultimate state is <name> score = 0.0 otherwise
    */
  def evaluate(query: String, data: AnalyzersData = AnalyzersData()): Result = {
    val listLength = data.item_list.length
    if(listLength >= 2 && data.item_list(listLength-2) === name) {
      Result(score = 1.0)
    } else {
      Result(score = 0.0)
    }
  }
}