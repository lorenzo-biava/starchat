package com.getjenny.starchat.resources

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 19/12/16.
  */

import akka.http.scaladsl.server.Route
import com.getjenny.starchat.entities._
import com.getjenny.starchat.routing._
import akka.pattern.CircuitBreaker
import scala.concurrent.{Future}
import akka.http.scaladsl.model.StatusCodes
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

trait RootAPIResource extends MyResource {
  def rootAPIsRoutes: Route = pathPrefix("") {
    pathEnd {
      get {
        val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
        onCompleteWithBreaker(breaker)(Future{Option(new RootAPIsDescription)}) {
          case Success(t) =>
            completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option{t})
          case Failure(e) =>
            log.error("route=RootRoutes method=GET: " + e.getMessage)
            completeResponse(StatusCodes.BadRequest,
              Option{ReturnMessageData(code = 100, message = e.getMessage)})
        }
      }
    }
  }
}



