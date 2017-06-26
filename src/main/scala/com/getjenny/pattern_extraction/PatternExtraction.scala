package com.getjenny.pattern_extraction

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 23/06/17.
  */

abstract class PatternExtraction(declaration: String) {
  def evaluate(input: String): Map[String, String]
}

case class PatternExtractionDeclarationParsingException(message: String = "", cause: Throwable = null)
  extends Exception(message, cause)

case class PatternExtractionParsingException(message: String = "", cause: Throwable = null)
  extends Exception(message, cause)

case class PatternExtractionBadSpecificationException(message: String = "", cause: Throwable = null)
  extends Exception(message, cause)
