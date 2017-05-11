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

trait TokenizersResource extends MyResource {
  val termService: TermService
  def esTokenizersRoutes: Route = pathPrefix("tokenizers") {
    pathEnd {
      post {
        entity(as[TokenizerQueryRequest]) { request_data =>
            val result: Try[Option[TokenizerResponse]] =
            Await.ready(Future { termService.esTokenizer(request_data)},
              60.seconds).value.get
          result match {
            case Success(t) =>
              completeResponse(StatusCodes.OK, StatusCodes.BadRequest,
                Future { Option { t } })
            case Failure(e) =>
              log.error("route=esTokenizersRoutes method=POST data=(" + request_data +
                ") : " + e.getMessage)
              completeResponse(StatusCodes.BadRequest)
          }
        }
      } ~
      {
        get {
          val analyzers_description: Map[String, String] =
            TokenizersDescription.analyzers_map.map(e => {
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
