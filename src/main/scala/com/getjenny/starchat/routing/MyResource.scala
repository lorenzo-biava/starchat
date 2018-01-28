package com.getjenny.starchat.routing

import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.marshalling.ToEntityMarshaller

import scala.concurrent.ExecutionContext
import akka.http.scaladsl.server.Directives
import com.getjenny.starchat.serializers.JsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Route
import com.getjenny.starchat.SCActorSystem
import com.getjenny.starchat.services.auth.StarChatAuthenticator
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

trait MyResource extends Directives with JsonSupport {

  implicit def executionContext: ExecutionContext

  val config: Config = ConfigFactory.load()
  val authRealm: String = config.getString("starchat.auth_realm")
  val authenticator = StarChatAuthenticator.authenticator
  val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)

  def completeResponse(status_code: StatusCode): Route = {
      complete(status_code)
  }

  def completeResponse[A: ToEntityMarshaller](statusCode: StatusCode, data: Option[A]): Route = {
    data match {
      case Some(t) =>
        val defaultHeader = RawHeader("application", "json")
        respondWithDefaultHeader(defaultHeader) {
          complete(statusCode, t)
        }
      case None =>
        complete(statusCode)
    }
  }

  def completeResponse[A: ToEntityMarshaller](statusCodeOk: StatusCode, statusCodeFailed: StatusCode,
                                              data: Option[A]): Route = {
    data match {
      case Some(t) =>
        val defaultHeader = RawHeader("application", "json")
        respondWithDefaultHeader(defaultHeader) {
          complete(statusCodeOk, t)
        }
      case None =>
        complete(statusCodeFailed)
    }
  }
}
