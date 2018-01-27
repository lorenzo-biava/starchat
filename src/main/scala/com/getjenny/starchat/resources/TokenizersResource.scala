package com.getjenny.starchat.resources

/**
  * Created by angelo on 03/04/17.
  */

import akka.http.scaladsl.server.Route
import com.getjenny.starchat.entities._
import com.getjenny.starchat.routing._

import scala.concurrent.Future
import akka.http.scaladsl.model.StatusCodes
import com.getjenny.starchat.services.TermService
import akka.pattern.CircuitBreaker
import scala.util.{Failure, Success}

trait TokenizersResource extends MyResource {
  def esTokenizersRoutes: Route =
    pathPrefix("""^(index_(?:[a-z]{1,256})_(?:[A-Za-z0-9_]{1,256}))$""".r ~ Slash ~ "tokenizers") { index_name =>
      pathEnd {
        post {
          authenticateBasicAsync(realm = authRealm,
            authenticator = authenticator.authenticator) { user =>
            authorizeAsync(_ =>
              authenticator.hasPermissions(user, index_name, Permissions.write)) {
              entity(as[TokenizerQueryRequest]) { request_data =>
                val termService = TermService
                val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
                onCompleteWithBreaker(breaker)(Future {
                  termService.esTokenizer(index_name, request_data)
                }) {
                  case Success(t) =>
                    completeResponse(StatusCodes.OK, StatusCodes.BadRequest, t)
                  case Failure(e) =>
                    log.error("index(" + index_name + ") route=esTokenizersRoutes method=POST data=(" +
                      request_data + ") : " + e.getMessage)
                    completeResponse(StatusCodes.BadRequest,
                      Option {
                        ReturnMessageData(code = 100, message = e.getMessage)
                      })
                }
              }
            }
          }
        } ~ {
          get {
            authenticateBasicAsync(realm = authRealm,
              authenticator = authenticator.authenticator) { user =>
              authorizeAsync(_ =>
                authenticator.hasPermissions(user, index_name, Permissions.read)) {
                val analyzers_description: Map[String, String] =
                  TokenizersDescription.analyzers_map.map(e => {
                    (e._1, e._2._2)
                  })
                val result: Option[Map[String, String]] = Option(analyzers_description)
                completeResponse(StatusCodes.OK, StatusCodes.BadRequest, result)
              }
            }
          }
        }
      }
    }
}
