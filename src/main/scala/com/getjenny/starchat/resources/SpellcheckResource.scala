package com.getjenny.starchat.resources

/**
  * Created by angelo on 21/04/17.
  */

import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.server.Route
import com.getjenny.starchat.entities._
import com.getjenny.starchat.routing._
import com.getjenny.starchat.services.SpellcheckService
import akka.http.scaladsl.model.StatusCodes
import com.getjenny.starchat.SCActorSystem
import akka.pattern.CircuitBreaker
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

trait SpellcheckResource extends MyResource {

  def spellcheckRoutes: Route = pathPrefix("spellcheck") {
    val spellcheckService = SpellcheckService
    pathPrefix("terms") {
      pathEnd {
        post {
          entity(as[SpellcheckTermsRequest]) { request =>
            val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
            onCompleteWithBreaker(breaker)(spellcheckService.termsSuggester(request)) {
              case Success(t) =>
                completeResponse(StatusCodes.OK, StatusCodes.BadRequest, t)
              case Failure(e) =>
                log.error("route=spellcheckRoutes method=POST: " + e.getMessage)
                completeResponse(StatusCodes.BadRequest,
                  Option{ReturnMessageData(code = 100, message = e.getMessage)})
            }
          }
        }
      }
    }
  }
}
