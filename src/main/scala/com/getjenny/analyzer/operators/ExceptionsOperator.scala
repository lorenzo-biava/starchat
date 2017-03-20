package com.getjenny.analyzer.operators

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 03/03/17.
  */

case class OperatorException(message: String = "", cause: Throwable = null)
  extends Exception(message, cause)

case class OperatorNotFoundException(message: String = "", cause: Throwable = null)
  extends Exception(message, cause)
