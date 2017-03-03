package com.getjenny.starchat.analyzer.atoms

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 03/03/17.
  */

case class AtomicException(message: String = "", cause: Throwable = null)
  extends Exception(message, cause)
