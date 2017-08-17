package com.getjenny.analyzer.atoms

import com.getjenny.analyzer.expressions.Result
import com.getjenny.analyzer.utils._
import com.getjenny.analyzer.expressions.Data
import com.getjenny.analyzer.expressions.Result


/**
  * Created by angelo on 26/06/17.
  */

/** test if a variable exists on dictionary of variables
  *
  * @param varname name of the variable to be checked
  */
class ExistsVariableAtomic(val varname: String) extends AbstractAtomic {
  override def toString: String = "existsVariable"
  val isEvaluateNormalized: Boolean = true

  /** Check if a variable named <varname> exists on the data variables dictionary
    *
    * @param query the user query
    * @param data the dictionary of variables (not used in this analyzer)
    * @return Result with 1.0 if the variable exists score = 0.0 otherwise
    */
  def evaluate(query: String, data: Data = Data()): Result = {
    if(data.extracted_variables.exists(_._1 == varname)) {
      Result(score = 1.0)
    } else {
      Result(score = 0.0)
    }
  }
}