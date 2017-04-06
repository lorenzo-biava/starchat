package com.getjenny.starchat.resources

/**
  * Created by angelo on 03/04/17.
  */

import akka.http.scaladsl.server.Route
import com.getjenny.starchat.entities._
import com.getjenny.starchat.routing.MyResource

import scala.concurrent.{Await, Future}
import akka.http.scaladsl.model.StatusCodes
import com.getjenny.starchat.services.TermService

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

trait AnalyzersResource extends MyResource {
  val termService: TermService
  def esAnalyzersRoutes: Route = pathPrefix("analyzers") {
    pathEnd {
      post {
        entity(as[AnalyzerQueryRequest]) { request_data =>
            val result: Try[Option[AnalyzerResponse]] =
            Await.ready(Future { termService.esAnalyzer(request_data)},
              30.seconds).value.get
          result match {
            case Success(t) =>
              completeResponse(StatusCodes.OK, StatusCodes.BadRequest,
                Future { Option { t } })
            case Failure(e) =>
              log.error("route=esAnalyzersRoutes method=POST data=(" + request_data +
                ") : " + e.getMessage)
              completeResponse(StatusCodes.BadRequest)
          }
        }
      } ~
      {
        get {
          val analyzers_description: Map[String, String] =
            AnalyzersDescription.analyzers_map.map(e => {
              (e._1, e._2._2)
            })
          val result: Future[Option[Map[String, String]]] =
            Future(Option(analyzers_description))
          completeResponse(StatusCodes.OK, StatusCodes.BadRequest, result)
        }
      }
    }
  }
}
