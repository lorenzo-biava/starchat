package com.getjenny.starchat.resources

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 19/12/16.
  */

import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.server.Route
import com.getjenny.starchat.entities._
import com.getjenny.starchat.routing.MyResource

import scala.concurrent.{Await, Future}
import akka.http.scaladsl.model.StatusCodes
import com.getjenny.starchat.services.IndexManagementService

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

trait IndexManagementResource extends MyResource {

  def indexManagementRoutes: Route = pathPrefix("index_management") {
    val indexManagementService = IndexManagementService
    path(Segment) { operation: String =>
      post
      {
        {
          operation match {
            case "refresh" =>
              val result: Try[Option[RefreshIndexResult]] =
                Await.ready(Future {indexManagementService.refresh_index()}, 10.seconds).value.get
              result match {
                case Success(t) => completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option {t})
                case Failure(e) => completeResponse(StatusCodes.BadRequest,
                  Option { IndexManagementResponse(message = e.getMessage) } )
              }
            case "create" =>
              val result: Try[Option[IndexManagementResponse]] =
                Await.ready(Future { indexManagementService.create_index() }, 10.seconds).value.get
              result match {
                case Success(t) => completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option {t})
                case Failure(e) => completeResponse(StatusCodes.BadRequest,
                  Option { IndexManagementResponse(message = e.getMessage) })
              }
            case _ => completeResponse(StatusCodes.BadRequest,
              Option{IndexManagementResponse(message = "Operation not supported: " + operation)})
          }
        }
      }
    } ~
    pathEnd {
      get {

        val result: Try[Option[IndexManagementResponse]] =
          Await.ready(Future{indexManagementService.check_index()}, 10.seconds).value.get
        result match {
          case Success(t) => completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option{t})
          case Failure(e) => completeResponse(StatusCodes.BadRequest,
            Option{IndexManagementResponse(message = e.getMessage)})
        }
      } ~
      delete {
        val result: Try[Option[IndexManagementResponse]] =
          Await.ready(Future{indexManagementService.remove_index()}, 10.seconds).value.get
        result match {
          case Success(t) => completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option{t})
          case Failure(e) => completeResponse(StatusCodes.BadRequest,
            Option{IndexManagementResponse(message = e.getMessage)})
        }
      } ~
      put {
        val result: Try[Option[IndexManagementResponse]] =
          Await.ready(Future{indexManagementService.update_index()}, 10.seconds).value.get
        result match {
          case Success(t) => completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option{t})
          case Failure(e) => completeResponse(StatusCodes.BadRequest,
            Option{IndexManagementResponse(message = e.getMessage)})
        }
      }
    }
  }
}



