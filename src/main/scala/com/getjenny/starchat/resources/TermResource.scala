package com.getjenny.starchat.resources

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 12/03/17.
  */

import akka.http.scaladsl.server.Route
import com.getjenny.starchat.entities._
import com.getjenny.starchat.routing._

import scala.concurrent.{Await, Future}
import akka.http.scaladsl.model.StatusCodes
import com.getjenny.starchat.services.TermService
import akka.pattern.CircuitBreaker

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

trait TermResource extends MyResource {

  def termRoutes: Route = pathPrefix("term") {
    val termService = TermService
    path(Segment) { operation: String =>
      post {
        operation match {
          case "index" =>
            parameters("refresh".as[Int] ? 0) { refresh =>
              entity(as[Terms]) { request_data =>
                val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
                onCompleteWithBreaker(breaker)(termService.index_term(request_data, refresh)) {
                  case Success(t) =>
                    completeResponse(StatusCodes.OK, StatusCodes.BadRequest, t)
                  case Failure(e) =>
                    log.error("route=termRoutes method=POST function=index : " + e.getMessage)
                    completeResponse(StatusCodes.BadRequest,
                      Option{ReturnMessageData(code = 100, message = e.getMessage)})
                }
              }
            }
          case "get" =>
            entity(as[TermIdsRequest]) { request_data =>
              val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
              onCompleteWithBreaker(breaker)(Future{termService.get_term(request_data)}) {
                case Success(t) =>
                  completeResponse(StatusCodes.OK, StatusCodes.BadRequest, t)
                case Failure(e) =>
                  log.error("route=termRoutes method=POST function=get : " + e.getMessage)
                  completeResponse(StatusCodes.BadRequest,
                    Option{ReturnMessageData(code = 101, message = e.getMessage)})
              }
            }
          case _ => completeResponse(StatusCodes.BadRequest,
            Option{IndexManagementResponse(message = "Operation not supported: " + operation)})
        }
      }
    } ~
    pathEnd {
      delete {
        parameters("refresh".as[Int] ? 0) { refresh =>
          entity(as[TermIdsRequest]) { request_data =>
            val termService = TermService
            if(request_data.ids.nonEmpty) {
              val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
              onCompleteWithBreaker(breaker)(termService.delete(request_data, refresh)) {
                case Success(t) =>
                  completeResponse(StatusCodes.OK, StatusCodes.BadRequest, t)
                case Failure(e) =>
                  log.error("route=termRoutes method=DELETE : " + e.getMessage)
                  completeResponse(StatusCodes.BadRequest,
                    Option{ReturnMessageData(code = 102, message = e.getMessage)})
              }
            } else {
              val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
              onCompleteWithBreaker(breaker)(termService.deleteAll()) {
                case Success(t) =>
                  completeResponse(StatusCodes.OK, StatusCodes.BadRequest, t)
                case Failure(e) =>
                  log.error("route=termRoutes method=DELETE : " + e.getMessage)
                  completeResponse(StatusCodes.BadRequest,
                    Option{ReturnMessageData(code = 103, message = e.getMessage)})
              }
            }
          }
        }
      } ~
      put {
        parameters("refresh".as[Int] ? 0) { refresh =>
          entity(as[Terms]) { request_data =>
            val termService = TermService
            val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
            onCompleteWithBreaker(breaker)(termService.update_term(request_data, refresh)) {
              case Success(t) =>
                completeResponse(StatusCodes.OK, StatusCodes.BadRequest, t)
              case Failure(e) =>
                log.error("route=termRoutes method=PUT : " + e.getMessage)
                completeResponse(StatusCodes.BadRequest, Option { IndexManagementResponse(message = e.getMessage) })
            }
          }
        }
      }
    } ~
    path(Segment) { operation: String =>
      get {
        operation match {
          case "term" =>
            entity(as[Term]) { request_data =>
              val termService = TermService
              val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
              onCompleteWithBreaker(breaker)(termService.search_term(request_data)) {
                case Success(t) =>
                  completeResponse(StatusCodes.OK, StatusCodes.BadRequest, t)
                case Failure(e) =>
                  log.error("route=termRoutes method=GET function=term : " + e.getMessage)
                  completeResponse(StatusCodes.BadRequest,
                    Option{IndexManagementResponse(message = e.getMessage)})
              }
            }
          case "text" =>
            entity(as[String]) { request_data =>
              val termService = TermService
              val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
              onCompleteWithBreaker(breaker)(termService.search(request_data)) {
                case Success(t) =>
                  completeResponse(StatusCodes.OK, StatusCodes.BadRequest, t)
                case Failure(e) =>
                  log.error("route=termRoutes method=GET function=text : " + e.getMessage)
                  completeResponse(StatusCodes.BadRequest,
                    Option{IndexManagementResponse(message = e.getMessage)})
              }
            }
        }
      }
    }
  }
}



