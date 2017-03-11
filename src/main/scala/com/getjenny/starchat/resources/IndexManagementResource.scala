package com.getjenny.starchat.resources

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 19/12/16.
  */

import akka.http.scaladsl.server.Route
import com.getjenny.starchat.entities._
import com.getjenny.starchat.routing.MyResource

import scala.concurrent.{Await, Future}
import akka.http.scaladsl.model.StatusCodes
import com.getjenny.starchat.services.IndexManagementService

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

trait IndexManagementResource extends MyResource {

  val indexManagementService: IndexManagementService

  def indexManagementRoutes: Route = pathPrefix("index_management") {
    pathEnd {
      post {
        val result: Future[Option[IndexManagementResponse]] = indexManagementService.create_index()
        val result_try: Try[Option[IndexManagementResponse]] = Await.ready(result,  30.seconds).value.get
        result_try match {
          case Success(t) => completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Future{Option{t}})
          case Failure(e) => completeResponse(StatusCodes.BadRequest,
            Future{Option{IndexManagementResponse(message = e.getMessage)}})
        }
      } ~
      get {
        val result: Future[Option[IndexManagementResponse]] = indexManagementService.check_index()
        val result_try: Try[Option[IndexManagementResponse]] = Await.ready(result,  30.seconds).value.get
        result_try match {
          case Success(t) => completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Future{Option{t}})
          case Failure(e) => completeResponse(StatusCodes.BadRequest,
            Future{Option{IndexManagementResponse(message = e.getMessage)}})
        }
        completeResponse(StatusCodes.OK, StatusCodes.BadRequest, result)
      } ~
      delete {
        val result: Future[Option[IndexManagementResponse]] = indexManagementService.remove_index()
        val result_try: Try[Option[IndexManagementResponse]] = Await.ready(result,  30.seconds).value.get
        result_try match {
          case Success(t) => completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Future{Option{t}})
          case Failure(e) => completeResponse(StatusCodes.BadRequest,
            Future{Option{IndexManagementResponse(message = e.getMessage)}})
        }
        completeResponse(StatusCodes.OK, StatusCodes.BadRequest, result)
      } ~
      put {
        val result: Future[Option[IndexManagementResponse]] = indexManagementService.update_index()
        val result_try: Try[Option[IndexManagementResponse]] = Await.ready(result,  30.seconds).value.get
        result_try match {
          case Success(t) => completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Future{Option{t}})
          case Failure(e) => completeResponse(StatusCodes.BadRequest,
            Future{Option{IndexManagementResponse(message = e.getMessage)}})
        }
        completeResponse(StatusCodes.OK, StatusCodes.BadRequest, result)
      }
    }
  }
}



