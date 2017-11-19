package com.getjenny.starchat.routing.auth

import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.directives.SecurityDirectives._
import com.getjenny.starchat.entities._
import com.getjenny.starchat.services.SystemIndexManagementService
import com.roundeights.hasher.Implicits._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

class BasicHttpStarchatAuthenticatorElasticSearch extends StarchatAuthenticator {
  val config: Config = ConfigFactory.load()
  val admin: String = config.getString("starchat.basic_http_es.admin")
  val password: String = config.getString("starchat.basic_http_es.password")
  val salt: String = config.getString("starchat.basic_http_es.salt")

  val systemAuthService: SystemIndexManagementService.type = SystemIndexManagementService

  def secret(password: String, salt: String): String = {
    password + "#" + salt
  }

  class Hasher(salt: String) {
    def hasher(password: String): String = {
      secret(password, salt).sha512
    }
  }

  def fetchUser(id: String): Future[User] = Future {
    id match {
      case `admin` =>
        User(id = admin, password = password, salt = salt, permissions = Map("admin" -> Set(Permissions.admin)))
      case _ =>
        User(
          id = "test_user", /** user id */
          password = "3c98bf19cb962ac4cd0227142b3495ab1be46534061919f792254b80c0f3e566f7819cae73bdc616af0ff555f7460ac96d88d56338d659ebd93e2be858ce1cf9", /** user password */
          salt = "salt", /** salt for password hashing */
          permissions = Map("index_0" -> Set(Permissions.read, Permissions.write))
        )
    }
  }

  val authenticator: AsyncAuthenticatorPF[User] = {
    case p @ Credentials.Provided(id) =>
      val user_request = Await.ready(fetchUser(id), 5.seconds).value.get
      user_request match {
        case Success(user) =>
          val hasher = new Hasher(user.salt)
          if(p.verify(secret = user.password, hasher = hasher.hasher)) {
            Future { user }
          } else {
            val message = "Authentication failed for the user: " + user.id
            throw AuthenticatorException(message)
          }
        case Failure(e) =>
          val message: String = "unable to match credentials"
          throw AuthenticatorException(message, e)
      }
  }

  def hasPermissions(user: User, index: String, permission: Permissions.Value): Future[Boolean] = {
    user.id match {
      case `admin` => //admin can do everything
        val user_permissions = user.permissions.getOrElse("admin", Set.empty[Permissions.Value])
        Future.successful(user_permissions.contains(Permissions.admin))
      case _ =>
        val user_permissions = user.permissions.getOrElse(index, Set.empty[Permissions.Value])
        Future.successful(user_permissions.contains(permission))
    }
  }
}