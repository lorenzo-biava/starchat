package com.getjenny.starchat.routing

import java.util.concurrent.TimeoutException

import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.{Directives, ExceptionHandler, Route}
import com.getjenny.starchat.SCActorSystem
import com.getjenny.starchat.serializers.JsonSupport
import com.getjenny.starchat.services.UserEsServiceException
import com.getjenny.starchat.services.auth.{AbstractStarChatAuthenticator, StarChatAuthenticator}
import com.typesafe.config.{Config, ConfigFactory}
import org.elasticsearch.index.IndexNotFoundException

import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal
import scala.util.matching.Regex
import com.getjenny.starchat.utils.Index

trait StarChatResource extends Directives with JsonSupport {

  implicit def executionContext: ExecutionContext
  val defaultHeader: RawHeader = RawHeader("application", "json")
  val config: Config = ConfigFactory.load()
  val authRealm: String = config.getString("starchat.auth_realm")
  val authenticator: AbstractStarChatAuthenticator = StarChatAuthenticator.authenticator
  val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)

  val indexRegex: Regex = Index.indexMatchRegexDelimited

  val routesExceptionHandler = ExceptionHandler {
    case e: IndexNotFoundException =>
      extractUri { uri =>
        log.error("uri(" + uri + ") index error: " + e)
        respondWithDefaultHeader(defaultHeader) {
          complete(StatusCodes.BadRequest)
        }
      }
    case e: TimeoutException =>
      extractUri { uri =>
        log.error("uri(" + uri + ") request timeout: " + e)
        respondWithDefaultHeader(defaultHeader) {
          complete(StatusCodes.RequestTimeout)
        }
      }
    case e: UserEsServiceException =>
      extractUri { uri =>
        log.error("uri(" + uri + ") Unauthorized: " + e)
        respondWithDefaultHeader(defaultHeader) {
          complete(StatusCodes.Unauthorized)
        }
      }
    case NonFatal(e) =>
      extractUri { uri =>
        log.error("uri(" + uri + ") Internal Error: " + e)
        respondWithDefaultHeader(defaultHeader) {
          complete(StatusCodes.BadRequest)
        }
      }
  }

  def completeResponse(status_code: StatusCode): Route = {
    complete(status_code)
  }

  def completeResponse[A: ToEntityMarshaller](statusCode: StatusCode, data: Option[A]): Route = {
    data match {
      case Some(t) =>
        respondWithDefaultHeader(defaultHeader) {
          complete(statusCode, t)
        }
      case None =>
        complete(statusCode)
    }
  }

  def completeResponse[A: ToEntityMarshaller](statusCode: StatusCode, data: A): Route = {
    respondWithDefaultHeader(defaultHeader) {
      complete(statusCode, data)
    }
  }

  def completeResponse[A: ToEntityMarshaller](statusCodeOk: StatusCode, statusCodeFailed: StatusCode,
                                              data: Option[A]): Route = {
    data match {
      case Some(t) =>
        respondWithDefaultHeader(defaultHeader) {
          complete(statusCodeOk, t)
        }
      case None =>
        complete(statusCodeFailed)
    }
  }

  def completeResponse[A: ToEntityMarshaller](statusCodeOk: StatusCode, statusCodeFailed: StatusCode,
                                              data: A): Route = {
    respondWithDefaultHeader(defaultHeader) {
      complete(statusCodeOk, data)
    }
  }

}
