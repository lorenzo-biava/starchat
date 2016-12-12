package com.getjenny.starchat.entities

/**
  * Created by Angelo Leto <angelo.leto@elegans.io> on 27/06/16.
  */

import scala.collection.immutable.{List, Map}

case class DTDocument(state: String,
                      queries: List[String],
                      bubble: String,
                      action: String,
                      action_input: Map[String, String],
                      success_value: String,
                      failure_value: String
                     )
