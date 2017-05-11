package com.getjenny.starchat.resources

/**
  * Created by angelo on 21/04/17.
  */

import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.server.Route
import com.getjenny.starchat.entities._
import com.getjenny.starchat.routing.MyResource
import com.getjenny.starchat.services.SpellcheckService
import akka.http.scaladsl.model.StatusCodes
import com.getjenny.starchat.SCActorSystem

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

trait SpellcheckResource extends MyResource {
  val spellcheckService: SpellcheckService

  def spellcheckRoutes: Route = pathPrefix("spellcheck") {
    pathPrefix("terms") {
      pathEnd {
        post {
          entity(as[SpellcheckTermsRequest]) { request =>
            val result: Try[Option[SpellcheckTermsResponse]] =
              Await.ready(Future{spellcheckService.termsSuggester(request)},
                60.seconds).value.get
            result match {
              case Success(t) =>
                completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Future {
                  Option {
                    t
                  }
                })
              case Failure(e) =>
                log.error("route=spellcheckRoutes method=POST: " + e.getMessage)
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
    }
  }
}
