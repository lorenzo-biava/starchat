package com.getjenny.starchat.entities

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 30/04/18.
  */

import scalaz.Scalaz._

object ObservedSearchDests extends Enumeration {
  type ObservedSearchDest = Value
  val KNOWLEDGEBASE, CONV_LOGS, unknown = Value
  def value(dest: String) = values.find(_.toString === dest).getOrElse(unknown)
}

