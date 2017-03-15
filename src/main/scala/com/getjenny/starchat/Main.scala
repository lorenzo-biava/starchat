package com.getjenny.starchat

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 27/06/16.
  */

import scala.concurrent.duration._
import akka.actor._
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout

import com.typesafe.config.ConfigFactory

object Main extends App with RestInterface {
  val config = ConfigFactory.load()

  val host = config.getString("http.host")
  val port = config.getInt("http.port")

  /* creation of the akka actor system which handle concurrent requests */
  implicit val system = ActorSystem("starchat-service")

  /* "The Materializer is a factory for stream execution engines, it is the thing that makes streams run" */
  implicit val materializer = ActorMaterializer()

  implicit val executionContext = system.dispatcher
  implicit val timeout = Timeout(10.seconds)

  val api = routes

  Http().bindAndHandle(handler = api, interface = host, port = port) map { binding =>
    system.log.info(s"REST interface bound to ${binding.localAddress}")
    } recover { case ex =>
    system.log.error(s"REST interface could not bind to $host:$port", ex.getMessage)
  }
}
