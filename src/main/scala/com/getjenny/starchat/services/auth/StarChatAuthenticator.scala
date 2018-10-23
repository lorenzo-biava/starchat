package com.getjenny.starchat.services.auth

import com.typesafe.config.{Config, ConfigFactory}
import com.getjenny.starchat.services._

object StarChatAuthenticator {
  val config: Config = ConfigFactory.load()
  val authMethodString: String = config.getString ("starchat.auth_method")
  val authMethod: SupportedAuthImpl.Value = SupportedAuthImpl.getValue (authMethodString)
  val authenticator = AuthenticatorFactory.apply(authMethod = authMethod, userService = UserService.service)
}
