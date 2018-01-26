package com.getjenny.starchat.resources

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 19/12/16.
  */

import akka.http.scaladsl.server.Route
import com.getjenny.starchat.entities._
import com.getjenny.starchat.routing._

import akka.http.scaladsl.model.StatusCodes
import com.getjenny.starchat.services.LanguageGuesserService
import akka.pattern.CircuitBreaker

import scala.util.{Failure, Success}

trait LanguageGuesserResource extends MyResource {

  def languageGuesserRoutes: Route =
    pathPrefix("""^(index_(?:[a-z]{1,256})_(?:[A-Za-z0-9_]{1,256}))$""".r ~ Slash ~ "language_guesser") { index_name =>
      val languageGuesserService = LanguageGuesserService
      pathEnd {
        post {
          authenticateBasicAsync(realm = auth_realm,
            authenticator = authenticator.authenticator) { user =>
            authorizeAsync(_ =>
              authenticator.hasPermissions(user, index_name, Permissions.read)) {
              entity(as[LanguageGuesserRequestIn]) { request_data =>
                val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
                onCompleteWithBreaker(breaker)(languageGuesserService.guessLanguage(index_name, request_data)) {
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
            authenticateBasicAsync(realm = auth_realm,
              authenticator = authenticator.authenticator) { user =>
              authorizeAsync(_ =>
                authenticator.hasPermissions(user, index_name, Permissions.read)) {
                val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
                onCompleteWithBreaker(breaker)(languageGuesserService.getLanguages(index_name, language)) {
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
