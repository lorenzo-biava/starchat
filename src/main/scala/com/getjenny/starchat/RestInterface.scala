package com.getjenny.starchat

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 27/06/16.
  */

import scala.concurrent.ExecutionContext

import akka.http.scaladsl.server.Route

import com.getjenny.starchat.resources._
import com.getjenny.starchat.services._

trait RestInterface extends Resources {
  implicit def executionContext: ExecutionContext

  lazy val kbElasticService = new KnowledgeBaseService
  lazy val dtElasticService = new DecisionTableService

  val routes: Route = rootAPIsRoutes ~ knowledgeBaseRoutes ~
    knowledgeBaseSearchRoutes ~ decisionTableRoutes ~ decisionTableSearchRoutes ~
    decisionTableResponseRequestRoutes
}

trait Resources extends KnowledgeBaseResource with DecisionTableResource with RootAPIResource
