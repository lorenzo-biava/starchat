package com.getjenny.starchat.routing.auth

import akka.http.scaladsl.server.directives.SecurityDirectives._
import com.getjenny.starchat.entities._

import scala.concurrent.Future

case class AuthenticatorException(message: String = "", cause: Throwable = null)
  extends Exception(message, cause)

trait StarchatAuthenticator {

  def fetchUser(id: String): Future[User]

  val authenticator: AsyncAuthenticatorPF[User]

  def hasPermissions(user: User, index: String, permission: Permissions.Value): Future[Boolean]

  def secret(password: String, salt: String): String

  def hashed_secret(password: String, salt: String): String
}
