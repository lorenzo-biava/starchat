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

case class Parameters(
         http_enable: Boolean,
         http_host: String,
         http_port: Int,
         https_enable: Boolean,
         https_host: String,
         https_port: Int,
         https_certificate: String,
         https_cert_pass: String)

class StarChatService(parameters: Option[Parameters] = None) extends RestInterface {
  val config = ConfigFactory.load()

  val params: Option[Parameters] = if(parameters.nonEmpty) {
    parameters
  } else {
    Option {
     Parameters(
        http_enable = config.getBoolean("http.enable"),
        http_host = config.getString("http.host"),
        http_port = config.getInt("http.port"),
        https_enable = config.getBoolean("https.enable"),
        https_host = config.getString("https.host"),
        https_port = config.getInt("https.port"),
        https_certificate = config.getString("https.certificate"),
        https_cert_pass = config.getString("https.password")
      )
    }
  }

  if(params.isEmpty) {
    log.error("cannot read parameters")
  }
  assert(params.nonEmpty)

  /* creation of the akka actor system which handle concurrent requests */
  implicit val system = SCActorSystem.system

  /* "The Materializer is a factory for stream execution engines, it is the thing that makes streams run" */
  implicit val materializer = ActorMaterializer()

  implicit val executionContext = system.dispatcher
  implicit val timeout = Timeout(10.seconds)

  val api = routes

  if (params.get.https_enable) {
    val password: Array[Char] = params.get.https_cert_pass.toCharArray

    val ks: KeyStore = KeyStore.getInstance("PKCS12")

    val keystore_path: String = "/tls/certs/" + params.get.https_certificate
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

    Http().bindAndHandle(handler = api, interface = params.get.https_host, params.get.https_port,
      connectionContext = https) map { binding =>
      system.log.info(s"REST (HTTPS) interface bound to ${binding.localAddress}")
    } recover { case ex =>
      system.log.error(s"REST (HTTPS) interface could not bind to ${params.get.http_host}:${params.get.http_port}",
        ex.getMessage)
    }
  }

  if((! params.get.https_enable) || params.get.http_enable) {
     Http().bindAndHandle(handler = api, interface = params.get.http_host,
       port = params.get.http_port) map { binding =>
      system.log.info(s"REST (HTTP) interface bound to ${binding.localAddress}")
    } recover { case ex =>
      system.log.error(s"REST (HTTP) interface could not bind to ${params.get.http_host}:${params.get.http_port}",
        ex.getMessage)
    }
  }

  /* try to initialize the analyzers, elasticsearch must be up and running */
  analyzerService.initializeAnalyzers()
}

object Main extends App {
  val starchatService = new StarChatService
}
