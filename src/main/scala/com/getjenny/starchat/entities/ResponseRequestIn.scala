package com.getjenny.starchat.entities

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 27/06/16.
  */

import scala.collection.immutable.Map

case class ResponseRequestInUserInput(text: Option[String], img: Option[String])

case class ResponseRequestIn(conversation_id: String,
                             traversed_states: Option[List[String]],
                             user_input: Option[ResponseRequestInUserInput],
                             state: Option[String],
                             data: Option[Map[String, String]],
                             threshold: Option[Double],
                             evaluation_class: Option[String],
                             max_results: Option[Int])
