package com.getjenny.starchat.resources

/**
  * Created by angelo on 03/04/17.
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

trait TokenizersResource extends MyResource {
  def esTokenizersRoutes: Route = pathPrefix("tokenizers") {
    pathEnd {
      post {
        entity(as[TokenizerQueryRequest]) { request_data =>
          val termService = TermService
          val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
          onCompleteWithBreaker(breaker)(Future{termService.esTokenizer(request_data)}) {
            case Success(t) =>
              completeResponse(StatusCodes.OK, StatusCodes.BadRequest, t)
            case Failure(e) =>
              log.error("route=esTokenizersRoutes method=POST data=(" + request_data + ") : " + e.getMessage)
              completeResponse(StatusCodes.BadRequest,
                Option{ReturnMessageData(code = 100, message = e.getMessage)})
          }
        }
      } ~
      {
        get {
          val analyzers_description: Map[String, String] =
            TokenizersDescription.analyzers_map.map(e => {
              (e._1, e._2._2)
            })
          val result: Option[Map[String, String]] = Option(analyzers_description)
          completeResponse(StatusCodes.OK, StatusCodes.BadRequest, result)
        }
      }
    }
  }
}
