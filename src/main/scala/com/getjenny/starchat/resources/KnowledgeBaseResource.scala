package com.getjenny.starchat.resources

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 27/06/16.
  */

import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.server.Route
import com.getjenny.starchat.entities._
import com.getjenny.starchat.routing._
import com.getjenny.starchat.services.KnowledgeBaseService
import akka.http.scaladsl.model.StatusCodes
import com.getjenny.starchat.SCActorSystem
import akka.pattern.CircuitBreaker

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

trait KnowledgeBaseResource extends MyResource {

  def knowledgeBaseRoutes: Route = pathPrefix("knowledgebase") {
    val knowledgeBaseService = KnowledgeBaseService
    pathEnd {
      post {
        parameters("refresh".as[Int] ? 0) { refresh =>
          entity(as[KBDocument]) { document =>
            val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
            onCompleteWithBreaker(breaker)(knowledgeBaseService.create(document, refresh)) {
              case Success(t) =>
                t match {
                  case Some(v) =>
                    completeResponse(StatusCodes.Created, StatusCodes.BadRequest, Option {v})
                  case None =>
                    log.error("route=knowledgeBaseRoutes method=POST")
                    completeResponse(StatusCodes.BadRequest,
                      Option {
                        ReturnMessageData(code = 100, message = "Error indexing new document, empty response")
                      })
                }
              case Failure(e) =>
                log.error("route=knowledgeBaseRoutes method=POST: " + e.getMessage)
                completeResponse(StatusCodes.BadRequest,
                Option{ReturnMessageData(code = 101, message = "Error indexing new document")})
            }
         }
        }
      } ~
        get {
          parameters("ids".as[String].*) { ids =>
            val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
            onCompleteWithBreaker(breaker)(knowledgeBaseService.read(ids.toList)) {
              case Success(t) =>
                completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option{t})
              case Failure(e) =>
                log.error("route=knowledgeBaseRoutes method=GET: " + e.getMessage)
                completeResponse(StatusCodes.BadRequest,
                  Option{ReturnMessageData(code = 102, message = e.getMessage)})
            }
          }
        } ~
        delete {
          val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
          onCompleteWithBreaker(breaker)(knowledgeBaseService.deleteAll()) {
            case Success(t) =>
              completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option{t})
            case Failure(e) =>
              log.error("route=knowledgeBaseRoutes method=DELETE: " + e.getMessage)
              completeResponse(StatusCodes.BadRequest,
                Option{ReturnMessageData(code = 103, message = e.getMessage)})
          }
        }
    } ~
      path(Segment) { id =>
        put {
          parameters("refresh".as[Int] ? 0) { refresh =>
            entity(as[KBDocumentUpdate]) { update =>
              val knowledgeBaseService = KnowledgeBaseService
              val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
              onCompleteWithBreaker(breaker)(knowledgeBaseService.update(id, update, refresh)) {
                case Success(t) =>
                  completeResponse(StatusCodes.Created, StatusCodes.BadRequest, t)
                case Failure(e) =>
                  log.error("route=knowledgeBaseRoutes method=PUT: " + e.getMessage)
                  completeResponse(StatusCodes.BadRequest,
                    Option{ReturnMessageData(code = 104, message = e.getMessage)})
              }
           }
          }
        } ~
          delete {
            parameters("refresh".as[Int] ? 0) { refresh =>
              val knowledgeBaseService = KnowledgeBaseService
              val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
              onCompleteWithBreaker(breaker)(knowledgeBaseService.delete(id, refresh)) {
                case Success(t) =>
                  completeResponse(StatusCodes.OK, StatusCodes.BadRequest, t)
                case Failure(e) =>
                  log.error("route=knowledgeBaseRoutes method=DELETE : " + e.getMessage)
                  completeResponse(StatusCodes.BadRequest,
                    Option{ReturnMessageData(code = 105, message = e.getMessage)})
              }
            }
          }
      }
  }

  def knowledgeBaseSearchRoutes: Route = pathPrefix("knowledgebase_search") {
    pathEnd {
      post {
        entity(as[KBDocumentSearch]) { docsearch =>
          val knowledgeBaseService = KnowledgeBaseService
          val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
          onCompleteWithBreaker(breaker)(knowledgeBaseService.search(docsearch)) {
            case Success(t) =>
              completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option{t})
            case Failure(e) =>
              log.error("route=decisionTableSearchRoutes method=POST: " + e.getMessage)
              completeResponse(StatusCodes.BadRequest,
                Option{ReturnMessageData(code = 106, message = e.getMessage)})
          }
        }
      }
    }
  }

}
