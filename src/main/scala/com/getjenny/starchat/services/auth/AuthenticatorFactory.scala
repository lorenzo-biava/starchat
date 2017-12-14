package com.getjenny.starchat.services.auth

import com.getjenny.starchat.services._

case class AuthenticatorClassNotFoundException(message: String = "", cause: Throwable = null)
  extends Exception(message, cause)

object SupportedAuthImpl extends Enumeration {
  type Permission = Value
  val basic_http, unknown = Value
  def getValue(auth_method: String): SupportedAuthImpl.Value =
    values.find(_.toString == auth_method).getOrElse(SupportedAuthImpl.unknown)
}

object AuthenticatorFactory {
  def apply(auth_method: SupportedAuthImpl.Value,
            userService: AbstractUserService): AbstractStarChatAuthenticator = {
    auth_method match {
      case SupportedAuthImpl.basic_http =>
        new BasicHttpStarChatAuthenticator(userService)
      case _ =>
        throw AuthenticatorClassNotFoundException("Authenticator not supported: " + auth_method)
    }
  }
}