package com.getjenny.starchat.resources

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 12/03/17.
  */

import akka.http.scaladsl.server.Route
import com.getjenny.starchat.entities._
import com.getjenny.starchat.routing.MyResource

import scala.concurrent.{Await, Future}
import akka.http.scaladsl.model.StatusCodes
import com.getjenny.starchat.services.TermService

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

trait TermResource extends MyResource {

  val termService: TermService

  def termRoutes: Route = pathPrefix("term") {
    path(Segment) { operation: String =>
      post {
        operation match {
          case "index" =>
            parameters("refresh".as[Int] ? 0) { refresh =>
              entity(as[Terms]) { request_data =>
                val result: Try[Option[IndexDocumentListResult]] =
                  Await.ready(Future{termService.index_term(request_data, refresh)}, 60.seconds).value.get
                result match {
                  case Success(t) =>
                    completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Future{Option{t}})
                  case Failure(e) =>
                    log.error("route=termRoutes method=POST function=index : " + e.getMessage)
                    completeResponse(StatusCodes.BadRequest,
                      Future{Option{IndexManagementResponse(message = e.getMessage)}})
                }
              }
            }
          case "get" =>
            entity(as[TermIdsRequest]) { request_data =>
              val result: Try[Option[Terms]] =
                Await.ready(Future{termService.get_term(request_data)}, 60.seconds).value.get
              result match {
                case Success(t) =>
                  completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Future{Option{t}})
                case Failure(e) =>
                  log.error("route=termRoutes method=POST function=get : " + e.getMessage)
                  completeResponse(StatusCodes.BadRequest,
                    Future{Option{IndexManagementResponse(message = e.getMessage)}})
              }
            }
          case _ => completeResponse(StatusCodes.BadRequest,
            Future{Option{IndexManagementResponse(message = "Operation not supported: " + operation)}})
        }
      }
    } ~
    pathEnd {
      delete {
        parameters("refresh".as[Int] ? 0) { refresh =>
          entity(as[TermIdsRequest]) { request_data =>
            val result: Try[Option[DeleteDocumentListResult]] =
              Await.ready(Future{termService.delete(request_data, refresh)}, 60.seconds).value.get
            result match {
              case Success(t) =>
                completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Future{Option{t}})
              case Failure(e) =>
                log.error("route=termRoutes method=DELETE : " + e.getMessage)
                completeResponse(StatusCodes.BadRequest,
                  Future{Option{IndexManagementResponse(message = e.getMessage)}})
            }
          }
        }
      } ~
      put {
        parameters("refresh".as[Int] ? 0) { refresh =>
          entity(as[Terms]) { request_data =>
            val result: Try[Option[UpdateDocumentListResult]] =
              Await.ready(Future {
                termService.update_term(request_data, refresh)
              }, 60.seconds).value.get
            result match {
              case Success(t) =>
                completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Future {
                  Option {
                    t
                  }
                })
              case Failure(e) =>
                log.error("route=termRoutes method=PUT : " + e.getMessage)
                completeResponse(StatusCodes.BadRequest,
                  Future {
                    Option {
                      IndexManagementResponse(message = e.getMessage)
                    }
                  })
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
              val result: Try[Option[TermsResults]] =
                Await.ready(Future{termService.search_term(request_data)}, 60.seconds).value.get
              result match {
                case Success(t) =>
                  completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Future{Option{t}})
                case Failure(e) =>
                  log.error("route=termRoutes method=GET function=term : " + e.getMessage)
                  completeResponse(StatusCodes.BadRequest,
                    Future{Option{IndexManagementResponse(message = e.getMessage)}})
              }
            }
          case "text" =>
            entity(as[String]) { request_data =>
              val result: Try[Option[TermsResults]] =
                Await.ready(Future{termService.search(request_data)}, 60.seconds).value.get
              result match {
                case Success(t) =>
                  completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Future{Option{t}})
                case Failure(e) =>
                  log.error("route=termRoutes method=GET function=text : " + e.getMessage)
                  completeResponse(StatusCodes.BadRequest,
                    Future{Option{IndexManagementResponse(message = e.getMessage)}})
              }
            }
        }
      }
    }
  }
}



