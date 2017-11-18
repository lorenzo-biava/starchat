package com.getjenny.starchat.services

import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.directives.AuthenticationDirective
import akka.http.scaladsl.server.directives.SecurityDirectives._

import akka.http.scaladsl.model.headers.BasicHttpCredentials
import scala.concurrent.{Await, Future}
import com.getjenny.starchat.entities._
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._
import com.roundeights.hasher.Implicits._

case class AuthenticatorException(message: String = "", cause: Throwable = null)
  extends Exception(message, cause)

trait AbstractAuthenticator {

  def fetchUser(id: String): Future[User]

  val authenticator: AsyncAuthenticatorPF[User]

  def hasPermissions(user: User, index: String, permission: Permissions.Value): Future[Boolean] = {
    val user_permissions = user.permissions.getOrElse(index, Set.empty[Permissions.Value])
    Future.successful(user_permissions.contains(permission))
  }
}
