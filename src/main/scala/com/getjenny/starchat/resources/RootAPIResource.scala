package com.getjenny.starchat.resources

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 19/12/16.
  */

import akka.http.scaladsl.server.Route
import com.getjenny.starchat.entities._
import com.getjenny.starchat.routing._
import akka.pattern.CircuitBreaker
import akka.http.scaladsl.model.StatusCodes
import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.duration._

trait RootAPIResource extends MyResource {
  def rootAPIsRoutes: Route = pathPrefix("") {
    pathEnd {
      get {
        val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker(maxFailure = 2, callTimeout = 1.second)
        onCompleteWithBreaker(breaker)(Future{None}) {
          case Success(v) =>
            completeResponse(StatusCodes.OK)
          case Failure(e) =>
            log.error("route=RootRoutes method=GET: " + e.getMessage)
            completeResponse(StatusCodes.BadRequest,
              Option{ReturnMessageData(code = 100, message = e.getMessage)})
        }
      }
    }
  }
}



