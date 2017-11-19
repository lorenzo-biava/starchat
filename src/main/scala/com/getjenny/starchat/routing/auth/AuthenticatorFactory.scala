package com.getjenny.starchat.routing.auth


case class AuthenticatorClassNotFoundException(message: String = "", cause: Throwable = null)
  extends Exception(message, cause)


object AuthenticatorFactory {
  def apply(auth_method: String): StarchatAuthenticator = {
    auth_method match {
      case "basic_http_es" =>
        new BasicHttpStarchatAuthenticatorElasticSearch
      case _ =>
        throw AuthenticatorClassNotFoundException("Authenticator not supported: " + auth_method)
    }
  }
}