package com.getjenny.starchat.entities

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 27/06/16.
  */

import scala.collection.immutable.Map

case class ResponseRequestInUserInput(text: Option[String], img: Option[String])

case class ResponseRequestIn(conversationId: String,
                             traversedStates: Option[Vector[String]],
                             userInput: Option[ResponseRequestInUserInput],
                             state: Option[String],
                             data: Option[Map[String, String]],
                             threshold: Option[Double],
                             evaluationClass: Option[String],
                             maxResults: Option[Int])
