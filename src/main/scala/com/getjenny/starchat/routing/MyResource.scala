package com.getjenny.starchat.routing

import akka.http.scaladsl.marshalling.{ToResponseMarshallable, ToEntityMarshaller}

import scala.concurrent.{ExecutionContext, Future}

import akka.http.scaladsl.server.{Directives, Route}

import com.getjenny.starchat.serializers.JsonSupport

import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.`Content-Type`
import akka.http.scaladsl.server.{ Route, ValidationRejection }

trait MyResource extends Directives with JsonSupport {

  implicit def executionContext: ExecutionContext

  def completeResponse(status_code: StatusCode): Route = {
      complete(status_code)
  }

  def completeResponse[A: ToEntityMarshaller](status_code: StatusCode, data: Future[Option[A]]): Route = {
    onSuccess(data) {
      case Some(t) =>
        complete(status_code, List(`Content-Type`(`application/json`)), t)
      case None =>
        complete(status_code)
    }
  }

  def completeResponse[A: ToEntityMarshaller](status_code_ok: StatusCode, status_code_failed: StatusCode,
                                     data: Future[Option[A]]): Route = {
    onSuccess(data) {
      case Some(t) =>
        complete(status_code_ok, List(`Content-Type`(`application/json`)), t)
      case None =>
        complete(status_code_failed)
    }
  }
}
