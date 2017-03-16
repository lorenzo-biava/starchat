package com.getjenny.starchat.routing

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.{ToEntityMarshaller, ToResponseMarshallable}

import scala.concurrent.{ExecutionContext, Future}
import akka.http.scaladsl.server.{Directives, Route}
import com.getjenny.starchat.serializers.JsonSupport
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.{Route, ValidationRejection}

trait MyResource extends Directives with JsonSupport {

  implicit def executionContext: ExecutionContext

  def completeResponse(status_code: StatusCode): Route = {
      complete(status_code)
  }

  def completeResponse[A: ToEntityMarshaller](status_code: StatusCode, data: Future[Option[A]]): Route = {
    onSuccess(data) {
      case Some(t) =>
        val blippy = RawHeader("application", "json")
        respondWithDefaultHeader(blippy) {
          complete(status_code, t)
        }
      case None =>
        complete(status_code)
    }
  }

  def completeResponse[A: ToEntityMarshaller](status_code_ok: StatusCode, status_code_failed: StatusCode,
                                     data: Future[Option[A]]): Route = {
    onSuccess(data) {
      case Some(t) =>
        val blippy = RawHeader("application", "json")
        respondWithDefaultHeader(blippy) {
          complete(status_code_ok, t)
        }
      case None =>
        complete(status_code_failed)
    }
  }
}
