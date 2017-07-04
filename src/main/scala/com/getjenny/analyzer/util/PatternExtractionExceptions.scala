package com.getjenny.analyzer.utils

/**
  * Created by angelo on 26/06/17.
  */

case class PatternExtractionDeclarationParsingException(message: String = "", cause: Throwable = null)
  extends Exception(message, cause)

case class PatternExtractionParsingException(message: String = "", cause: Throwable = null)
  extends Exception(message, cause)

case class PatternExtractionNoMatchException(message: String = "", cause: Throwable = null)
  extends Exception(message, cause)

case class PatternExtractionBadSpecificationException(message: String = "", cause: Throwable = null)
  extends Exception(message, cause)