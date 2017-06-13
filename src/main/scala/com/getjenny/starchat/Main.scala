package com.getjenny.starchat

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 27/06/16.
  */

import scala.concurrent.duration._
import akka.actor._
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import java.io.InputStream
import java.security.{ SecureRandom, KeyStore }
import javax.net.ssl.{ SSLContext, TrustManagerFactory, KeyManagerFactory }

import akka.actor.ActorSystem
import akka.http.scaladsl.server.{ Route, Directives }
import akka.http.scaladsl.{ ConnectionContext, HttpsConnectionContext, Http }
import akka.stream.ActorMaterializer
import com.typesafe.sslconfig.akka.AkkaSSLConfig
import com.typesafe.config.ConfigFactory

object Main extends App with RestInterface {
  val config = ConfigFactory.load()

  val http_enable = config.getBoolean("http.enable")
  val http_host = config.getString("http.host")
  val http_port = config.getInt("http.port")

  val https_enable = config.getBoolean("https.enable")
  val https_host = config.getString("https.host")
  val https_port = config.getInt("https.port")
  val https_certificate = config.getString("https.certificate")
  val https_cert_pass = config.getString("https.password")

  /* creation of the akka actor system which handle concurrent requests */
  implicit val system = SCActorSystem.system

  /* "The Materializer is a factory for stream execution engines, it is the thing that makes streams run" */
  implicit val materializer = ActorMaterializer()

  implicit val executionContext = system.dispatcher
  implicit val timeout = Timeout(10.seconds)

  val api = routes

  if (https_enable) {
    val password: Array[Char] = https_cert_pass.toCharArray

    val ks: KeyStore = KeyStore.getInstance("PKCS12")

    val keystore_path: String = "/tls/certs/" + https_certificate
    val keystore: InputStream = getClass.getResourceAsStream(keystore_path)
    //val keystore: InputStream = getClass.getClassLoader.getResourceAsStream(keystore_path)

    require(keystore != null, "Keystore required!")
    ks.load(keystore, password)

    val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, password)

    val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
    tmf.init(ks)

    val sslContext: SSLContext = SSLContext.getInstance("TLS")
    sslContext.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom)
    val https: HttpsConnectionContext = ConnectionContext.https(sslContext)

    Http().bindAndHandle(handler = api, interface = https_host, https_port, connectionContext = https) map { binding =>
     system.log.info(s"REST (HTTPS) interface bound to ${binding.localAddress}")
    } recover { case ex =>
      system.log.error(s"REST (HTTPS) interface could not bind to $http_host:$http_port", ex.getMessage)
    }
  }

  if((! https_enable) || http_enable) {
     Http().bindAndHandle(handler = api, interface = http_host, port = http_port) map { binding =>
      system.log.info(s"REST (HTTP) interface bound to ${binding.localAddress}")
    } recover { case ex =>
      system.log.error(s"REST (HTTP) interface could not bind to $http_host:$http_port", ex.getMessage)
    }
  }

  /* try to initialize the analyzers, elasticsearch must be up and running */
  analyzerService.initializeAnalyzers()
}
