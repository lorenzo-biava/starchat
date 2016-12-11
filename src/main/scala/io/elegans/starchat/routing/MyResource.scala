package com.getjenny.starchat.routing

import akka.http.scaladsl.marshalling.{ToResponseMarshaller, ToResponseMarshallable}

import scala.concurrent.{ExecutionContext, Future}

import akka.http.scaladsl.server.{Directives, Route}

import com.getjenny.starchat.serializers.JsonSupport
import com.getjenny.starchat.entities._


trait MyResource extends Directives with JsonSupport {

  implicit def executionContext: ExecutionContext

  def completeResponseMessageData(status_code_ok: Int, message: Future[Option[ReturnMessageData]]): Route = {
    onSuccess(message) {
      case Some(t) => complete(ToResponseMarshallable(status_code_ok, message))
      case None =>
      complete(ToResponseMarshallable(status_code_ok,
        ReturnMessageData(code = 400, message = "error evaluating evaluating response")))
    }
  }

  def completeResponseRequestOutJson(status_code_ok: Int, status_code_failed: Int,
                                     data: Option[ResponseRequestOut]): Route = {
    data match {
      case Some(t) => complete(ToResponseMarshallable(status_code_ok, t))
      case None => complete(ToResponseMarshallable(status_code_failed,
        ReturnMessageData(code = 400, message = "error evaluating response")))
    }
  }

  def completeSearchKBDocumentResultsJson(status_code_ok: Int, status_code_failed: Int,
                                          data: Future[Option[SearchKBDocumentsResults]]): Route = {
    onSuccess(data) {
      case Some(t) => complete(ToResponseMarshallable(status_code_ok, t))
      case None => complete(status_code_failed, None)
    }
  }

  def completeSearchDTDocumentResultsJson(status_code_ok: Int, status_code_failed: Int,
                                          data: Future[Option[SearchDTDocumentsResults]]): Route = {
    onSuccess(data) {
      case Some(t) => complete(ToResponseMarshallable(status_code_ok, t))
      case None => complete(status_code_failed, None)
    }
  }

  def completeDTDocumentJson(status_code_ok: Int, status_code_failed: Int,
                             data: Future[Option[DTDocument]]): Route = {
    onSuccess(data) {
      case Some(t) => complete(ToResponseMarshallable(status_code_ok, t))
      case None => complete(status_code_failed, None)
    }
  }

  def completeUpdateDocumentResultJson(status_code_ok: Int, status_code_failed: Int,
                                    data: Future[Option[UpdateDocumentResult]]): Route = {
    onSuccess(data) {
      case Some(t) => complete(ToResponseMarshallable(status_code_ok, t))
      case None => complete(status_code_failed, None)
    }
  }

  def completeDeleteDocumentResultJson(status_code_ok: Int, status_code_failed: Int,
                                       data: Future[Option[DeleteDocumentResult]]): Route = {
    onSuccess(data) {
      case Some(t) =>
        if(t.found) {
          complete(ToResponseMarshallable(status_code_ok, t))
        } else {
          complete(ToResponseMarshallable(status_code_failed, t))
        }
      case None => complete(status_code_failed, None)
    }
  }

  def completeIndexDocumentJson(status_code_ok: Int, status_code_failed: Int,
                                    data: Future[Option[IndexDocumentResult]]): Route = {
    onSuccess(data) {
      case Some(t) => complete(ToResponseMarshallable(status_code_ok, t))
      case None => complete(status_code_failed, None)
    }
  }

}
