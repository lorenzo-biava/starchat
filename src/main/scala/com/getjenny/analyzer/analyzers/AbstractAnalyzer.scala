package com.getjenny.analyzer.analyzers

/**
  * Created by mal on 20/02/2017.
  */

abstract class AbstractAnalyzer(command_string: String) {
  def evaluate(sentence: String): Double
}