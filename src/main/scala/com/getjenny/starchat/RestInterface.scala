package com.getjenny.starchat

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 27/06/16.
  */

import scala.concurrent.ExecutionContext
import akka.actor.ActorSystem
import akka.http.scaladsl.server.Route

import com.getjenny.starchat.resources._
import com.getjenny.starchat.services._

trait Resources extends KnowledgeBaseResource with DecisionTableResource
  with RootAPIResource with IndexManagementResource with LanguageGuesserResource
  with TermResource with ESAnalyzersResource

trait RestInterface extends Resources {
  implicit def executionContext: ExecutionContext

  lazy val kbElasticService = new KnowledgeBaseService
  lazy val dtElasticService = new DecisionTableService
  lazy val indexManagementService = new IndexManagementService
  lazy val languageGuesserService = new LanguageGuesserService
  lazy val termService = new TermService

  val routes: Route = rootAPIsRoutes ~ knowledgeBaseRoutes ~
    knowledgeBaseSearchRoutes ~ decisionTableRoutes ~ decisionTableSearchRoutes ~
    decisionTableResponseRequestRoutes ~ decisionTableAnalyzerRoutes ~ indexManagementRoutes ~
    languageGuesserRoutes ~ termRoutes ~ esAnalyzersRoutes
}
