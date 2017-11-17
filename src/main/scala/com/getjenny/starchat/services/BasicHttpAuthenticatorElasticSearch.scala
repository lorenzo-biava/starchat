package com.getjenny.starchat.services

import akka.http.scaladsl.server.directives.Credentials
import scala.concurrent.{Await, Future}
import com.getjenny.starchat.entities._
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._

class BasicHttpAuthenticatorElasticSearch extends AbstractAuthenticator {

  def fetchUser(id: String): Future[Option[User]] {

  }

  val authenticator: AsyncAuthenticatorPF[User] = {
    case p @ Credentials.Provided(id) if p.verify("p4ssw0rd") =>
      fetchUser(id)
  }

}