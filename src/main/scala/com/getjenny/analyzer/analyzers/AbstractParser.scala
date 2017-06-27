package com.getjenny.analyzer.analyzers

import com.getjenny.analyzer.expressions.Result

/**
  * Created by mal on 20/02/2017.
  */

abstract class AbstractParser(command_string: String) {
  def evaluate(sentence: String): Result
}