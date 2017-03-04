package com.getjenny.starchat.entities

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 27/06/16.
  */

import scala.collection.immutable.Map

case class ResponseRequestInUserInput(text: Option[String], img: Option[String])

case class ResponseRequestInValues(return_value: Option[String], data: Option[Map[String, String]])

case class ResponseRequestIn(conversation_id: Option[String], user_input: Option[ResponseRequestInUserInput],
                                values: Option[ResponseRequestInValues], min_score: Option[Float],
                                boost_exact_match_factor: Option[Float])
