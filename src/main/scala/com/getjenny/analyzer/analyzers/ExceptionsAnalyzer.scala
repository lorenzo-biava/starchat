package com.getjenny.analyzer.analyzers

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 03/03/17.
  */

case class AnalyzerInitializationException(message: String = "", cause: Throwable = null)
  extends Exception(message, cause)

case class AnalyzerParsingException(message: String = "", cause: Throwable = null)
  extends Exception(message, cause)

case class AnalyzerCommandException(message: String = "", cause: Throwable = null)
  extends Exception(message, cause)

case class AnalyzerEvaluationException(message: String = "", cause: Throwable = null)
  extends Exception(message, cause)
