package com.getjenny.starchat.entities

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 27/06/16.
  */

import scala.collection.immutable.{List, Map}

case class DTDocumentUpdate(queries: Option[List[String]],
                            bubble: Option[String],
                            action: Option[String],
                            action_input: Option[Map[String, String]],
                            success_value: Option[String],
                            failure_value: Option[String]
                            )
