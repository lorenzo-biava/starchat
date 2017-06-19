package com.getjenny.starchat.resources

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 27/06/16.
  */

import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.server.Route
import com.getjenny.starchat.entities._
import com.getjenny.starchat.routing.MyResource
import com.getjenny.starchat.services.DecisionTableService
import com.getjenny.starchat.services.ResponseService
import com.getjenny.starchat.services.AnalyzerService
import akka.http.scaladsl.model.StatusCodes
import com.getjenny.starchat.SCActorSystem
import com.getjenny.analyzer.analyzers._


import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

import akka.http.scaladsl.server.directives.DebuggingDirectives
import akka.http.scaladsl.server.directives.LoggingMagnet
import akka.http.scaladsl.server.directives.LogEntry
import akka.http.scaladsl.model.{HttpRequest}
import akka.http.scaladsl.server.RouteResult

trait DecisionTableResource extends MyResource {

  def requestAsInfo(req: HttpRequest): LogEntry = LogEntry(req, Logging.DebugLevel)
  val logRequestAsInfo = DebuggingDirectives.logRequest(requestAsInfo _)

  def responseAsInfo(res: RouteResult): LogEntry = LogEntry(res, Logging.DebugLevel)
  val logResponseAsInfo = DebuggingDirectives.logResult(responseAsInfo _)

  // logs just the request method and response status at info level
  def requestMethodAndResponseStatus(req: HttpRequest): RouteResult => Option[LogEntry] = {
    case RouteResult.Complete(res) => Some(LogEntry(req.method.name + ": " + res.status, Logging.InfoLevel))
    case RouteResult.Rejected(rejections) ⇒
      Some(LogEntry(s"Request: $req\nwas rejected with rejections:\n$rejections", Logging.WarningLevel))
    case _ ⇒ Some(LogEntry(s"Request: $req\n has an unknown statusrejected", Logging.ErrorLevel))
  }
  def printRequestMethodAndResponseStatus(req: HttpRequest)(res: RouteResult): Unit =
    LogEntry(requestMethodAndResponseStatus(req)(res).map(_.obj.toString).getOrElse(""))
  val logRequestResultPrintln = DebuggingDirectives.logRequestResult(LoggingMagnet(_ => printRequestMethodAndResponseStatus))


  ///def decisionTableResponseRequestRoutes: Route = logResponseAsInfo(logRequestAsInfo(decisionTableResponseRequestRoutes2))
  def decisionTableResponseRequestRoutes: Route = logRequestResultPrintln(decisionTableResponseRequestRoutes2)

  val decisionTableService: DecisionTableService
  val responseService: ResponseService
  val analyzerService: AnalyzerService

  def decisionTableRoutes: Route = pathPrefix("decisiontable") {
    pathEnd {
      post {
        parameters("refresh".as[Int] ? 0) { refresh =>
          entity(as[DTDocument]) { document =>
            val result: Future[Option[IndexDocumentResult]] = decisionTableService.create(document, refresh)
            completeResponse(StatusCodes.Created, StatusCodes.BadRequest, result)
          }
        }
      } ~
        get {
          parameters("ids".as[String].*) { ids =>
            val result: Future[Option[SearchDTDocumentsResults]] = decisionTableService.read(ids.toList)
            completeResponse(StatusCodes.OK, StatusCodes.BadRequest, result)
          }
        }
    } ~
      path(Segment) { id =>
        put {
          entity(as[DTDocumentUpdate]) { update =>
            parameters("refresh".as[Int] ? 0) { refresh =>
              val result: Future[Option[UpdateDocumentResult]] = decisionTableService.update(id, update, refresh)
              val result_try: Try[Option[UpdateDocumentResult]] = Await.ready(result,  60.seconds).value.get
              result_try match {
                case Success(t) =>
                  completeResponse(StatusCodes.Created, StatusCodes.BadRequest, Future{Option{t}})
                case Failure(e) =>
                  completeResponse(StatusCodes.BadRequest,
                    Future{Option{ReturnMessageData(code = 101, message = e.getMessage)}})
              }
            }
          }
        } ~
          delete {
            parameters("refresh".as[Int] ? 0) { refresh =>
              val result: Future[Option[DeleteDocumentResult]] = decisionTableService.delete(id, refresh)
              onSuccess(result) {
                case Some(t) =>
                  if(t.found) {
                    completeResponse(StatusCodes.OK, result)
                  } else {
                    completeResponse(StatusCodes.BadRequest, result)
                  }
                case None => completeResponse(StatusCodes.BadRequest)
              }
            }
          }
      }
  }

  def decisionTableAnalyzerRoutes: Route = pathPrefix("decisiontable_analyzer") {
    pathEnd {
      get {
        val result: Try[Option[DTAnalyzerMap]] =
          Await.ready(analyzerService.getDTAnalyzerMap, 60.seconds).value.get
        result match {
          case Success(t) =>
            completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Future{Option{t}})
          case Failure(e) =>
            log.error("route=decisionTableAnalyzerRoutes method=GET: " + e.getMessage)
            completeResponse(StatusCodes.BadRequest,
              Future{Option{IndexManagementResponse(message = e.getMessage)}})
        }
      } ~
        post {
          val result: Try[Option[DTAnalyzerLoad]] =
            Await.ready(analyzerService.loadAnalyzer, 60.seconds).value.get
          result match {
            case Success(t) =>
              completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Future{Option{t}})
            case Failure(e) =>
              log.error("route=decisionTableAnalyzerRoutes method=POST: " + e.getMessage)
              completeResponse(StatusCodes.BadRequest,
                Future{Option{IndexManagementResponse(message = e.getMessage)}})
          }
        }
    }
  }

  def decisionTableSearchRoutes: Route = pathPrefix("decisiontable_search") {
    pathEnd {
      post {
        entity(as[DTDocumentSearch]) { docsearch =>
          val result: Future[Option[SearchDTDocumentsResults]] = decisionTableService.search(docsearch)
          completeResponse(StatusCodes.OK, StatusCodes.BadRequest, result)
        }
      }
    }
  }

  def decisionTableResponseRequestRoutes2: Route = pathPrefix("get_next_response") {
    pathEnd {
      post {
        entity(as[ResponseRequestIn])
        {
          response_request =>
            val response: Try[Option[ResponseRequestOutOperationResult]] =
              Await.ready(responseService.getNextResponse(response_request), 60.seconds).value.get
            response match {
              case Failure(e) =>
                log.error("DecisionTableResource: Unable to complete the request: " + e.getMessage)
                completeResponse(StatusCodes.BadRequest,
                  Future {
                    Option {
                      ResponseRequestOutOperationResult(
                        ReturnMessageData(code = 102, message = e.getMessage),
                        Option{ List.empty[ResponseRequestOut] })
                    }
                  }
                )
              case Success(response_value) =>
                response_value match {
                  case Some(t) =>
                    if (t.status.code == 200) {
                      completeResponse(StatusCodes.OK, StatusCodes.Gone, Future{t.response_request_out})
                    }  else {
                      completeResponse(StatusCodes.NoContent) // no response found
                    }
                  case None =>
                    log.error("DecisionTableResource: Unable to complete the request")
                    completeResponse(StatusCodes.BadRequest,
                      Future {
                        Option {
                          ResponseRequestOutOperationResult(
                            ReturnMessageData(code = 101, message = "unable to complete the response"),
                            Option{ List.empty[ResponseRequestOut] })
                        }
                      }
                    )
                }
            }
        }
      }
    }
  }
}



