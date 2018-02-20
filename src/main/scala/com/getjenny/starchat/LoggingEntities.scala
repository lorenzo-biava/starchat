package com.getjenny.starchat.resources

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 27/06/16.
  */

import java.util.Base64

import akka.event.Logging
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server
import akka.http.scaladsl.server.RouteResult
import akka.http.scaladsl.server.directives.{DebuggingDirectives, LogEntry}
import com.typesafe.config.{Config, ConfigFactory}

object LoggingEntities {
  val config: Config = ConfigFactory.load()
  def responseStatus(req: HttpRequest): RouteResult => Option[LogEntry] = {
    case RouteResult.Complete(res) =>
      Some(LogEntry("ReqUri(" + req.uri + ") ReqMethodRes(" + req.method.name + ":" + res.status + ")",
        Logging.InfoLevel))
    case _ => Option.empty[LogEntry]
  }

  def requestMethodAndResponseStatus(req: HttpRequest): RouteResult => Option[LogEntry] = {
    case RouteResult.Complete(res) =>
      Some(LogEntry("ReqUri(" + req.uri + ")" +
        " ReqMethodRes(" + req.method.name + ":" + res.status + ")" +
        " ReqEntity(" + req.entity.toString + ") ResEntity(" +
        res.entity.toString + ") ", Logging.InfoLevel))
    case _ => Option.empty[LogEntry]
  }

  def requestMethodAndResponseStatusB64(req: HttpRequest): RouteResult => Option[LogEntry] = {
    case RouteResult.Complete(res) =>
      Some(LogEntry("ReqUri(" + req.uri + ")" +
        " ReqMethodRes(" + req.method.name + ":" + res.status + ")" +
        " ReqEntity(" + req.entity + ")" +
        " ReqB64Entity(" + Base64.getEncoder.encodeToString(req.entity.toString.getBytes) + ")" +
        " ResEntity(" + res.entity + ")" +
        " ResB64Entity(" + Base64.getEncoder.encodeToString(res.entity.toString.getBytes) + ")",
        Logging.InfoLevel))
    case _ => Option.empty[LogEntry]
  }

  val logRequestAndResult: server.Directive0 =
    DebuggingDirectives.logRequestResult(requestMethodAndResponseStatus _)
  val logRequestAndResultB64: server.Directive0 =
    DebuggingDirectives.logRequestResult(requestMethodAndResponseStatusB64 _)
  val logRequestAndResultReduced: server.Directive0 =
    DebuggingDirectives.logRequestResult(responseStatus _)
}



