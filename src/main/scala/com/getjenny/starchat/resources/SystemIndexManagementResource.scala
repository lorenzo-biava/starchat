package com.getjenny.starchat.resources

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 14/11/16.
  */

import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.server.Route
import com.getjenny.starchat.entities._
import com.getjenny.starchat.routing._

import scala.concurrent.{Await, Future}
import akka.http.scaladsl.model.StatusCodes
import com.getjenny.starchat.services.SystemIndexManagementService

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import akka.pattern.CircuitBreaker

trait SystemIndexManagementResource extends MyResource {

  def systemIndexManagementRoutes: Route = pathPrefix("system_index_management") {
    val indexManagementService = SystemIndexManagementService
    path(Segment) { operation: String =>
      post
      {
        {
          operation match {
            case "refresh" =>
              val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
              onCompleteWithBreaker(breaker)(indexManagementService.refresh_index()) {
                case Success(t) => completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option {t})
                case Failure(e) => completeResponse(StatusCodes.BadRequest,
                  Option { IndexManagementResponse(message = e.getMessage) } )
              }
            case "create" =>
              val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
              onCompleteWithBreaker(breaker)(indexManagementService.create_index()) {
                case Success(t) => completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option {t})
                case Failure(e) => completeResponse(StatusCodes.BadRequest,
                  Option { IndexManagementResponse(message = e.getMessage) })
              }
            case _ => completeResponse(StatusCodes.BadRequest,
              Option{IndexManagementResponse(message = "index(system) Operation not supported: " + operation)})
          }
        }
      }
    } ~
    pathEnd {
      get {
        val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
        onCompleteWithBreaker(breaker)(indexManagementService.check_index()) {
          case Success(t) => completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option{t})
          case Failure(e) => completeResponse(StatusCodes.BadRequest,
            Option{IndexManagementResponse(message = e.getMessage)})
        }
      } ~
      delete {
        val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
        onCompleteWithBreaker(breaker)(indexManagementService.remove_index()) {
          case Success(t) => completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option{t})
          case Failure(e) => completeResponse(StatusCodes.BadRequest,
            Option{IndexManagementResponse(message = e.getMessage)})
        }
      } ~
      put {
        val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
        onCompleteWithBreaker(breaker)(indexManagementService.update_index()) {
          case Success(t) => completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option{t})
          case Failure(e) => completeResponse(StatusCodes.BadRequest,
            Option{IndexManagementResponse(message = e.getMessage)})
        }
      }
    }
  }
}


