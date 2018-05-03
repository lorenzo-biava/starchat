package com.getjenny.starchat.services

import com.typesafe.config.{Config, ConfigFactory}


object UserService {
  val config: Config = ConfigFactory.load ()
  val authCredentialStoreString: String = config.getString ("starchat.auth_credential_store")
  val authCredentialStore: SupportedAuthCredentialStoreImpl.Value =
    SupportedAuthCredentialStoreImpl.getValue (authCredentialStoreString)

  val service = UserFactory.apply(userCredentialStore = authCredentialStore)
}