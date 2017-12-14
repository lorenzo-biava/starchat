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
  val auth_realm: String = config.getString("starchat.auth_realm")
  val authenticator = StarChatAuthenticator.authenticator
  val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)

  def completeResponse(status_code: StatusCode): Route = {
      complete(status_code)
  }

  def completeResponse[A: ToEntityMarshaller](status_code: StatusCode, data: Option[A]): Route = {
    data match {
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
                                     data: Option[A]): Route = {
    data match {
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
