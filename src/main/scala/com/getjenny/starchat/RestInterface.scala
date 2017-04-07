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
  with TermResource with TokenizersResource with AnalyzersPlaygroundResource

trait RestInterface extends Resources {
  implicit def executionContext: ExecutionContext

  lazy val knowledgeBaseService = new KnowledgeBaseService
  lazy val decisionTableService = new DecisionTableService
  lazy val indexManagementService = new IndexManagementService
  lazy val languageGuesserService = new LanguageGuesserService
  lazy val termService = new TermService
  lazy val responseService = new ResponseService
  lazy val analyzerService = new AnalyzerService

  val routes: Route = rootAPIsRoutes ~ knowledgeBaseRoutes ~
    knowledgeBaseSearchRoutes ~ decisionTableRoutes ~ decisionTableSearchRoutes ~
    decisionTableResponseRequestRoutes ~ decisionTableAnalyzerRoutes ~ indexManagementRoutes ~
    languageGuesserRoutes ~ termRoutes ~ esTokenizersRoutes ~ analyzersPlaygroundRoutes
}
