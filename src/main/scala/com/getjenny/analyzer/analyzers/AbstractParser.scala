package com.getjenny.analyzer.analyzers

/**
  * Created by mal on 20/02/2017.
  */

abstract class AbstractParser(command_string: String) {
  def evaluate(sentence: String): Double
}