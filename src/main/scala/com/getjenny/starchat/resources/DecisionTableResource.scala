package com.getjenny.starchat.resources

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 27/06/16.
  */

import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.server.Route
import com.getjenny.starchat.entities._
import com.getjenny.starchat.routing._
import com.getjenny.starchat.services.DecisionTableService
import com.getjenny.starchat.services.ResponseService
import com.getjenny.starchat.services.AnalyzerService
import akka.http.scaladsl.model.StatusCodes
import akka.pattern.CircuitBreaker
import com.getjenny.starchat.SCActorSystem
import com.getjenny.analyzer.analyzers._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

trait DecisionTableResource extends MyResource {

  def decisionTableRoutes: Route = pathPrefix("decisiontable") {
    pathEnd {
      val decisionTableService = DecisionTableService
      post {
        parameters("refresh".as[Int] ? 0) { refresh =>
          entity(as[DTDocument]) { document =>
            val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
            onCompleteWithBreaker(breaker)(decisionTableService.create(document, refresh)) {
              case Success(t) =>
                completeResponse(StatusCodes.Created, StatusCodes.BadRequest, Option{t})
              case Failure(e) =>
                log.error("route=decisionTableRoutes method=POST: " + e.getMessage)
                completeResponse(StatusCodes.BadRequest,
                  Option{ReturnMessageData(code = 100, message = e.getMessage)})
            }
          }
        }
      } ~
        get {
          parameters("ids".as[String].*, "dump".as[Boolean] ? false) { (ids, dump) =>
            if(dump == false) {
              val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
              onCompleteWithBreaker(breaker)(decisionTableService.read(ids.toList)) {
                case Success(t) =>
                  completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option{t})
                case Failure(e) =>
                  log.error("route=decisionTableRoutes method=GET: " + e.getMessage)
                  completeResponse(StatusCodes.BadRequest,
                    Option{ReturnMessageData(code = 101, message = e.getMessage)})
              }
            } else {
              val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
              onCompleteWithBreaker(breaker)(decisionTableService.getDTDocuments()) {
                case Success(t) =>
                  completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option{t})
                case Failure(e) =>
                  log.error("route=decisionTableRoutes method=GET: " + e.getMessage)
                  completeResponse(StatusCodes.BadRequest,
                    Option{ReturnMessageData(code = 102, message = e.getMessage)})
              }
            }
          }
        } ~
        delete {
          val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
          onCompleteWithBreaker(breaker)(decisionTableService.deleteAll()) {
            case Success(t) =>
              completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option{t})
            case Failure(e) =>
              log.error("route=decisionTableRoutes method=DELETE : " + e.getMessage)
              completeResponse(StatusCodes.BadRequest,
                Option{ReturnMessageData(code = 103, message = e.getMessage)})
          }
        }
    } ~
      path(Segment) { id =>
        put {
          entity(as[DTDocumentUpdate]) { update =>
            parameters("refresh".as[Int] ? 0) { refresh =>
              val decisionTableService = DecisionTableService
              val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
              onCompleteWithBreaker(breaker)(decisionTableService.update(id, update, refresh)) {
                case Success(t) =>
                  completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option{t})
                case Failure(e) =>
                  log.error("route=decisionTableRoutes method=PUT : " + e.getMessage)
                  completeResponse(StatusCodes.BadRequest,
                    Option{ReturnMessageData(code = 104, message = e.getMessage)})
              }
            }
          }
        } ~
          delete {
            parameters("refresh".as[Int] ? 0) { refresh =>
              val decisionTableService = DecisionTableService
              val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
              onCompleteWithBreaker(breaker)(decisionTableService.delete(id, refresh)) {
                case Success(t) =>
                  if(t.isDefined) {
                    completeResponse(StatusCodes.OK, t)
                  } else {
                    completeResponse(StatusCodes.BadRequest, t)
                  }
                case Failure(e) =>
                  log.error("route=decisionTableRoutes method=DELETE : " + e.getMessage)
                  completeResponse(StatusCodes.BadRequest,
                    Option{ReturnMessageData(code = 105, message = e.getMessage)})
              }
            }
          }
      }
  }

  def decisionTableAnalyzerRoutes: Route = pathPrefix("decisiontable_analyzer") {
    pathEnd {
      get {
        val analyzerService = AnalyzerService
        val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
        onCompleteWithBreaker(breaker)(analyzerService.getDTAnalyzerMap) {
          case Success(t) =>
            completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option{t})
          case Failure(e) =>
            log.error("route=decisionTableAnalyzerRoutes method=GET: " + e.getMessage)
            completeResponse(StatusCodes.BadRequest,
              Option{ReturnMessageData(code = 106, message = e.getMessage)})
        }
      } ~
        post {
          val analyzerService = AnalyzerService
          val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
          onCompleteWithBreaker(breaker)(analyzerService.loadAnalyzer(propagate = true)) {
            case Success(t) =>
              completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option{t})
            case Failure(e) =>
              log.error("route=decisionTableAnalyzerRoutes method=POST: " + e.getMessage)
              completeResponse(StatusCodes.BadRequest,
                Option{ReturnMessageData(code = 107, message = e.getMessage)})
          }
        }
    }
  }

  def decisionTableSearchRoutes: Route = pathPrefix("decisiontable_search") {
    pathEnd {
      post {
        entity(as[DTDocumentSearch]) { docsearch =>
          val decisionTableService = DecisionTableService
          val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
          onCompleteWithBreaker(breaker)(decisionTableService.search(docsearch)) {
            case Success(t) =>
              completeResponse(StatusCodes.Created, StatusCodes.BadRequest, Option{t})
            case Failure(e) =>
              log.error("route=decisionTableSearchRoutes method=POST: " + e.getMessage)
              completeResponse(StatusCodes.BadRequest,
                Option{ReturnMessageData(code = 108, message = e.getMessage)})
          }
        }
      }
    }
  }

  def decisionTableResponseRequestRoutes: Route = pathPrefix("get_next_response") {
    pathEnd {
      post {
        entity(as[ResponseRequestIn])
        {
          response_request =>
            val responseService = ResponseService
            val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
            onCompleteWithBreaker(breaker)(responseService.getNextResponse(response_request)) {
              case Failure(e) =>
                log.error("DecisionTableResource: Unable to complete the request: " + e.getMessage)
                completeResponse(StatusCodes.BadRequest,
                  Option {
                    ResponseRequestOutOperationResult(
                      ReturnMessageData(code = 109, message = e.getMessage),
                      Option{ List.empty[ResponseRequestOut] })
                  }
                )
              case Success(response_value) =>
                response_value match {
                  case Some(t) =>
                    if (t.status.code == 200) {
                      completeResponse(StatusCodes.OK, StatusCodes.Gone, t.response_request_out)
                    }  else {
                      completeResponse(StatusCodes.NoContent) // no response found
                    }
                  case None =>
                    log.error("DecisionTableResource: Unable to complete the request")
                    completeResponse(StatusCodes.BadRequest,
                      Option {
                        ResponseRequestOutOperationResult(
                          ReturnMessageData(code = 110, message = "unable to complete the response"),
                          Option{ List.empty[ResponseRequestOut] })
                      }
                    )
                }
            }
        }
      }
    }
  }
}

