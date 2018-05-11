package com.getjenny.starchat.entities

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 30/04/18.
  */

import scalaz.Scalaz._

object TermSearchModes extends Enumeration {
  type TermSearchMode = Value
  val TERMS_COMMON_ONLY, TERMS_IDXSPECIFIC_ONLY, unknown = Value
  def value(TermSearchMode: String) = values.find(_.toString === TermSearchMode).getOrElse(unknown)
}
