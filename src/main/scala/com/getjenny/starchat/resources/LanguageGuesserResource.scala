package com.getjenny.starchat.resources

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 19/12/16.
  */

import akka.http.scaladsl.server.Route
import com.getjenny.starchat.entities._
import com.getjenny.starchat.routing.MyResource

import scala.concurrent.{Await, Future}
import akka.http.scaladsl.model.StatusCodes
import com.getjenny.starchat.services.LanguageGuesserService

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

trait LanguageGuesserResource extends MyResource {

  val languageGuesserService: LanguageGuesserService

  def languageGuesserRoutes: Route = pathPrefix("language_guesser") {
    pathEnd {
      post {
        entity(as[LanguageGuesserRequestIn]) { request_data =>
          val result: Future[Option[LanguageGuesserRequestOut]] = languageGuesserService.guess_language(request_data)
          val result_try: Try[Option[LanguageGuesserRequestOut]] = Await.ready(result, 30.seconds).value.get
          result_try match {
            case Success(t) => completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Future {
              Option {
                t
              }
            })
            case Failure(e) => completeResponse(StatusCodes.BadRequest,
              Future {
                Option {
                  IndexManagementResponse(message = e.getMessage)
                }
              })
          }
        }
      }
    } ~
    path(Segment) { language: String =>
      get {
        val result: Future[Option[LanguageGuesserInformations]] = languageGuesserService.get_languages(language)
        val result_try: Try[Option[LanguageGuesserInformations]] = Await.ready(result, 30.seconds).value.get
        result_try match {
          case Success(t) => completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Future {Option {t}})
          case Failure(e) => completeResponse(StatusCodes.BadRequest, Future {
              Option {
                IndexManagementResponse(message = e.getMessage)
              }
            })
        }
        completeResponse(StatusCodes.OK, StatusCodes.BadRequest, result)
      }
    }
  }
}



