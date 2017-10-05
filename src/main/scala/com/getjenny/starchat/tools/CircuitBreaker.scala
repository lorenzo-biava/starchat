package com.getjenny.starchat.tools

import scala.concurrent.duration._
import akka.pattern.CircuitBreaker
import com.getjenny.starchat.SCActorSystem
import scala.concurrent.ExecutionContext.Implicits.global

object StarChatCircuitBreaker {
  def getCircuitBreaker(maxFailure: Int = 10, callTimeout: FiniteDuration = 10.seconds,
                        resetTimeout: FiniteDuration = 30.seconds): CircuitBreaker = {
    val breaker = new CircuitBreaker(scheduler = SCActorSystem.system.scheduler,
      maxFailures = maxFailure,
      callTimeout = callTimeout,
      resetTimeout = resetTimeout
    )
    breaker
  }

  def callWithBreaker(): Unit = {

  }
}


