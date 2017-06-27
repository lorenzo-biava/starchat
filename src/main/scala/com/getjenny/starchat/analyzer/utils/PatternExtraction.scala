package com.getjenny.starchat.analyzer.utils

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 23/06/17.
  */

abstract class PatternExtraction(declaration: String) {
  def evaluate(input: String): Map[String, String]
}

