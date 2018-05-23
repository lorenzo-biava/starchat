package com.getjenny.starchat.entities

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 30/04/18.
  */

import scalaz.Scalaz._

object StatSearchModes extends Enumeration {
  type TermSearchMode = Value
  val STAT_COMMON_KNOWLEDGEBASE, STAT_COMMON_CONV_LOGS,
    STAT_IDXSPECIFIC_KNOWLEDGEBASE, STAT_IDXSPECIFIC_LOGS, unknown = Value
  def value(statSearchMode: String) = values.find(_.toString === statSearchMode).getOrElse(unknown)
}

