package com.getjenny.starchat.services

import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.directives.AuthenticationDirective
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import scala.concurrent.{Await, Future}
import com.getjenny.starchat.entities._
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._

case class AuthenticatorPermissionDeniedException(message: String = "", cause: Throwable = null)
  extends Exception(message, cause)

case class AuthenticatorException(message: String = "", cause: Throwable = null)
  extends Exception(message, cause)

case class AuthorizationException(message: String = "", cause: Throwable = null)
  extends Exception(message, cause)

trait AbstractAuthenticator {

  type AsyncAuthenticatorPF[T] = PartialFunction[Credentials, Future[T]]

  def authenticator[T](realm: String, authenticator: AsyncAuthenticatorPF[T]): AuthenticationDirective[T]

  def fetchUser(id: String): Future[Option[User]]

  def hasPermissions(id: String, index: String, permission: Permissions.Value): Future[Boolean] = {
    val user: Try[Option[User]] =
      Await.ready(fetchUser(id = id), 5.seconds).value.get
    user match {
      case Success(t) =>
        val user_permissions = t.get.permissions.getOrElse(index, Set.empty[Permissions.Value])
        Future{user_permissions.contains(permission)}
      case Failure(e) =>
        throw AuthorizationException("Authorization failed: id(" + id + ") ; The operation requires permissions: " +
          permission + " ; " + e.getMessage)
    }
  }
}
