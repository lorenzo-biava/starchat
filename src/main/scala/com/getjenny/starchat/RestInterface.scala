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
  with SpellcheckResource

trait RestInterface extends Resources {
  implicit def executionContext: ExecutionContext

  lazy val knowledgeBaseService = new KnowledgeBaseService
  lazy val decisionTableService = new DecisionTableService
  lazy val indexManagementService = new IndexManagementService
  lazy val languageGuesserService = new LanguageGuesserService
  lazy val termService = new TermService
  lazy val responseService = new ResponseService
  lazy val analyzerService = new AnalyzerService
  lazy val spellcheckService = new SpellcheckService

  val routes: Route = LoggingEntities.logRequestAndResult(rootAPIsRoutes) ~
    LoggingEntities.logRequestAndResult(knowledgeBaseRoutes) ~
    LoggingEntities.logRequestAndResultB64(knowledgeBaseSearchRoutes) ~
    LoggingEntities.logRequestAndResultB64(decisionTableRoutes) ~
    LoggingEntities.logRequestAndResultB64(decisionTableSearchRoutes) ~
    LoggingEntities.logRequestAndResultB64(decisionTableResponseRequestRoutes) ~
    LoggingEntities.logRequestAndResult(decisionTableAnalyzerRoutes) ~
    LoggingEntities.logRequestAndResult(indexManagementRoutes) ~
    LoggingEntities.logRequestAndResult(languageGuesserRoutes) ~
    LoggingEntities.logRequestAndResultReduced(termRoutes) ~
    LoggingEntities.logRequestAndResult(esTokenizersRoutes) ~
    LoggingEntities.logRequestAndResult(analyzersPlaygroundRoutes) ~
    LoggingEntities.logRequestAndResult(spellcheckRoutes)
}
