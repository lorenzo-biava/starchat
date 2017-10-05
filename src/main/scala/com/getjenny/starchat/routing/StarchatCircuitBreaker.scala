package com.getjenny.starchat.routing

import akka.event.LoggingAdapter
import akka.http.scaladsl.model.StatusCodes
import akka.pattern.CircuitBreaker
import com.getjenny.starchat.SCActorSystem
import com.getjenny.starchat.entities.ReturnMessageData

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

object StarChatCircuitBreaker {
  def getCircuitBreaker(maxFailure: Int = 10, callTimeout: FiniteDuration = 10.seconds,
                        resetTimeout: FiniteDuration = 2.seconds): CircuitBreaker = {
    val breaker = new CircuitBreaker(scheduler = SCActorSystem.system.scheduler,
      maxFailures = maxFailure,
      callTimeout = callTimeout,
      resetTimeout = resetTimeout
    )
    breaker
  }
}


