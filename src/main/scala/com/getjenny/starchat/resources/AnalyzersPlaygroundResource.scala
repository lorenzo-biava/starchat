package com.getjenny.starchat.resources

/**
  * Created by angelo on 07/04/17.
  */

import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.server.Route
import com.getjenny.starchat.entities._
import com.getjenny.starchat.routing.MyResource
import com.getjenny.starchat.services.AnalyzerService
import akka.http.scaladsl.model.StatusCodes
import com.getjenny.starchat.SCActorSystem

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

trait AnalyzersPlaygroundResource extends MyResource {
  val analyzerService: AnalyzerService

  def analyzersPlaygroundRoutes: Route = pathPrefix("analyzers_playground") {
    pathEnd {
      post {
       entity(as[AnalyzerEvaluateRequest]) { request =>
         val result = Try(analyzerService.evaluateAnalyzer(request))
         result match {
           case Success(t) =>
             completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Future {
               Option {
                 t
               }
             })
           case Failure(e) =>
             log.error("route=analyzersPlaygroundRoutes method=POST: " + e.getMessage)
             completeResponse(StatusCodes.BadRequest,
               Future {
                 Option {
                   ReturnMessageData(code=100, message = e.getMessage)
                 }
               })
         }
       }
      }
    }
  }
}



