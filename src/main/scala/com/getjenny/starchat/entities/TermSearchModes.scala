package com.getjenny.starchat.entities

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 30/04/18.
  */

import scalaz.Scalaz._

object TermSearchModes extends Enumeration {
  type TermSearchMode = Value
  val TERMS_COMMON, TERMS_IDXSPECIFIC, unknown = Value
  def value(termSearchMode: String) = values.find(_.toString === termSearchMode).getOrElse(unknown)
}
