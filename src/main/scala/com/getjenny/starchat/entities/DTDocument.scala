package com.getjenny.starchat.entities

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 27/06/16.
  */

import scala.collection.immutable.{List, Map}

case class DTDocument(state: String,
                      execution_order: Int,
                      max_state_count: Int,
                      analyzer: String,
                      queries: List[String],
                      bubble: String,
                      action: String,
                      action_input: Map[String, String],
                      state_data: Map[String, String],
                      success_value: String,
                      failure_value: String
                     )
