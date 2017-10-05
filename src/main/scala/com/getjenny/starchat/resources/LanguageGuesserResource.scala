package com.getjenny.starchat.resources

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 19/12/16.
  */

import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.server.Route
import com.getjenny.starchat.entities._
import com.getjenny.starchat.routing._

import scala.concurrent.{Await, Future}
import akka.http.scaladsl.model.StatusCodes
import com.getjenny.starchat.SCActorSystem
import com.getjenny.starchat.services.LanguageGuesserService
import akka.pattern.CircuitBreaker

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

trait LanguageGuesserResource extends MyResource {

  def languageGuesserRoutes: Route = pathPrefix("language_guesser") {
    val languageGuesserService = LanguageGuesserService
    pathEnd {
      post {
        entity(as[LanguageGuesserRequestIn]) { request_data =>
          val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
          onCompleteWithBreaker(breaker)(languageGuesserService.guess_language(request_data)) {
            case Success(t) =>
              completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option{t})
            case Failure(e) =>
              log.error("route=languageGuesserRoutes method=POST: " + e.getMessage)
              completeResponse(StatusCodes.BadRequest,
                Option{ReturnMessageData(code = 100, message = e.getMessage)})
          }
        }
      }
    } ~
    path(Segment) { language: String =>
      get {
        val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
        onCompleteWithBreaker(breaker)(languageGuesserService.get_languages(language)) {
            case Success(t) =>
              completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option{t})
            case Failure(e) =>
              log.error("route=languageGuesserRoutes method=GET: " + e.getMessage)
              completeResponse(StatusCodes.BadRequest,
                Option{ReturnMessageData(code = 101, message = e.getMessage)})
          }
      }
    }
  }
}



