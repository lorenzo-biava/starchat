package com.getjenny.starchat.routing

import akka.pattern.CircuitBreaker
import com.getjenny.starchat.SCActorSystem

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

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


