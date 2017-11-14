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
import com.getjenny.starchat.services.IndexManagementService

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import akka.pattern.CircuitBreaker

trait IndexManagementResource extends MyResource {

  def indexManagementRoutes: Route =
    pathPrefix("""^(index_(?:[A-Za-z0-9_]+))$""".r ~ Slash ~ "index_management") { index_name =>
    val indexManagementService = IndexManagementService
    path(Segment) { operation: String =>
      post
      {
        {
          operation match {
            case "refresh" =>
              val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
              onCompleteWithBreaker(breaker)(indexManagementService.refresh_index(index_name)) {
                case Success(t) => completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option {t})
                case Failure(e) => completeResponse(StatusCodes.BadRequest,
                  Option { IndexManagementResponse(message = e.getMessage) } )
              }
            case "create" =>
              val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
              onCompleteWithBreaker(breaker)(indexManagementService.create_index(index_name)) {
                case Success(t) => completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option {t})
                case Failure(e) => completeResponse(StatusCodes.BadRequest,
                  Option { IndexManagementResponse(message = e.getMessage) })
              }
            case _ => completeResponse(StatusCodes.BadRequest,
              Option{IndexManagementResponse(message = "index(" + index_name + ") Operation not supported: " + operation)})
          }
        }
      }
    } ~
    pathEnd {
      get {
        val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
        onCompleteWithBreaker(breaker)(indexManagementService.check_index(index_name)) {
          case Success(t) => completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option{t})
          case Failure(e) => completeResponse(StatusCodes.BadRequest,
            Option{IndexManagementResponse(message = e.getMessage)})
        }
      } ~
      delete {
        val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
        onCompleteWithBreaker(breaker)(indexManagementService.remove_index(index_name)) {
          case Success(t) => completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option{t})
          case Failure(e) => completeResponse(StatusCodes.BadRequest,
            Option{IndexManagementResponse(message = e.getMessage)})
        }
      } ~
      put {
        val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
        onCompleteWithBreaker(breaker)(indexManagementService.update_index(index_name)) {
          case Success(t) => completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option{t})
          case Failure(e) => completeResponse(StatusCodes.BadRequest,
            Option{IndexManagementResponse(message = e.getMessage)})
        }
      }
    }
  }
}



