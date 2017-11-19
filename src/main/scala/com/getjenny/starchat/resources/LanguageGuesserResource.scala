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
import com.getjenny.starchat.services.{BasicHttpStarchatAuthenticatorElasticSearch, LanguageGuesserService}
import akka.pattern.CircuitBreaker

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

trait LanguageGuesserResource extends MyResource {

  def languageGuesserRoutes: Route =
    pathPrefix("""^(index_(?:[A-Za-z0-9_]+))$""".r ~ Slash ~ "language_guesser") { index_name =>
      val languageGuesserService = LanguageGuesserService
      pathEnd {
        post {
          authenticateBasicPFAsync(realm = "starchat",
            authenticator = authenticator.authenticator) { user =>
            authorizeAsync(_ =>
              authenticator.hasPermissions(user, index_name, Permissions.read)) {
              entity(as[LanguageGuesserRequestIn]) { request_data =>
                val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
                onCompleteWithBreaker(breaker)(languageGuesserService.guess_language(index_name, request_data)) {
                  case Success(t) =>
                    completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option {
                      t
                    })
                  case Failure(e) =>
                    log.error("index(" + index_name + ") route=languageGuesserRoutes method=POST: " + e.getMessage)
                    completeResponse(StatusCodes.BadRequest,
                      Option {
                        ReturnMessageData(code = 100, message = e.getMessage)
                      })
                }
              }
            }
          }
        }
      } ~
        path(Segment) { language: String =>
          get {
            authenticateBasicPFAsync(realm = "starchat",
              authenticator = authenticator.authenticator) { user =>
              authorizeAsync(_ =>
                authenticator.hasPermissions(user, index_name, Permissions.create)) {
                val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
                onCompleteWithBreaker(breaker)(languageGuesserService.get_languages(index_name, language)) {
                  case Success(t) =>
                    completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option {
                      t
                    })
                  case Failure(e) =>
                    log.error("index(" + index_name + ") route=languageGuesserRoutes method=GET: " + e.getMessage)
                    completeResponse(StatusCodes.BadRequest,
                      Option {
                        ReturnMessageData(code = 101, message = e.getMessage)
                      })
                }
              }
            }
          }
        }
    }
}
