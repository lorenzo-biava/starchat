package com.getjenny.starchat.entities

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 27/06/16.
  */

import scala.collection.immutable.{List, Map}

case class DTDocumentUpdate(execution_order: Option[Int],
                            max_state_count: Option[Int],
                            analyzer: Option[String],
                            queries: Option[List[String]],
                            bubble: Option[String],
                            action: Option[String],
                            action_input: Option[Map[String, String]],
                            state_data: Option[Map[String, String]],
                            success_value: Option[String],
                            failure_value: Option[String]
                            )
