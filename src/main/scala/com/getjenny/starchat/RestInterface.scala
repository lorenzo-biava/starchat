package com.getjenny.starchat

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 27/06/16.
  */

import akka.http.scaladsl.server.Route
import com.getjenny.starchat.resources._
import com.getjenny.starchat.services._

import scala.concurrent.ExecutionContext

trait RestInterface extends KnowledgeBaseResource with DecisionTableResource with RootAPIResource
  with SystemIndexManagementResource with IndexManagementResource with LanguageGuesserResource
  with TermResource with TokenizersResource with AnalyzersPlaygroundResource with TermsExtractionResource
  with SpellcheckResource with ConversationLogsResource with PriorDataResource with UserResource {

  implicit def executionContext: ExecutionContext

  lazy val knowledgeBaseService = KnowledgeBaseService
  lazy val decisionTableService = DecisionTableService
  lazy val indexManagementService = IndexManagementService
  lazy val systemIndexManagementService = SystemIndexManagementService
  lazy val languageGuesserService = LanguageGuesserService
  lazy val termService = TermService
  lazy val responseService = ResponseService
  lazy val analyzerService = AnalyzerService
  lazy val userService = UserService
  lazy val spellcheckService = SpellcheckService
  lazy val cronReloadDTService = CronReloadDTService
  lazy val cronCleanDTService = CronCleanDTService
  lazy val systemService = DtReloadService
  lazy val conversationLogsService = ConversationLogsService
  lazy val statTextService = PriorDataService

  val routes: Route = rootAPIsRoutes ~
    LoggingEntities.logRequestAndResultReduced(kbQuestionAnswerRoutes) ~
    LoggingEntities.logRequestAndResultReduced(kbQuestionAnswerStreamRoutes) ~
    LoggingEntities.logRequestAndResult(kbQuestionAnswerSearchRoutes) ~
    LoggingEntities.logRequestAndResult(kbTotalTermsRoutes) ~
    LoggingEntities.logRequestAndResult(kbDictSizeRoutes) ~
    LoggingEntities.logRequestAndResult(kbTermsCountRoutes) ~
    LoggingEntities.logRequestAndResultReduced(pdQuestionAnswerRoutes) ~
    LoggingEntities.logRequestAndResultReduced(pdQuestionAnswerStreamRoutes) ~
    LoggingEntities.logRequestAndResult(pdQuestionAnswerSearchRoutes) ~
    LoggingEntities.logRequestAndResult(pdTotalTermsRoutes) ~
    LoggingEntities.logRequestAndResult(pdDictSizeRoutes) ~
    LoggingEntities.logRequestAndResult(pdTermsCountRoutes) ~
    LoggingEntities.logRequestAndResultReduced(clQuestionAnswerRoutes) ~
    LoggingEntities.logRequestAndResultReduced(clQuestionAnswerStreamRoutes) ~
    LoggingEntities.logRequestAndResult(clQuestionAnswerSearchRoutes) ~
    LoggingEntities.logRequestAndResult(clTotalTermsRoutes) ~
    LoggingEntities.logRequestAndResult(clDictSizeRoutes) ~
    LoggingEntities.logRequestAndResult(clTermsCountRoutes) ~
    LoggingEntities.logRequestAndResult(termsExtractionRoutes) ~
    LoggingEntities.logRequestAndResult(synExtractionRoutes) ~
    LoggingEntities.logRequestAndResult(decisionTableRoutes) ~
    LoggingEntities.logRequestAndResult(decisionTableUploadCSVRoutes) ~
    LoggingEntities.logRequestAndResult(decisionTableSearchRoutes) ~
    LoggingEntities.logRequestAndResult(decisionTableAsyncReloadRoutes) ~
    LoggingEntities.logRequestAndResult(decisionTableResponseRequestRoutes) ~
    LoggingEntities.logRequestAndResult(decisionTableAnalyzerRoutes) ~
    LoggingEntities.logRequestAndResult(postIndexManagementCreateRoutes) ~
    LoggingEntities.logRequestAndResult(postIndexManagementRefreshRoutes) ~
    LoggingEntities.logRequestAndResult(putIndexManagementRoutes) ~
    LoggingEntities.logRequestAndResult(indexManagementRoutes) ~
    LoggingEntities.logRequestAndResult(postIndexManagementOpenCloseRoutes) ~
    LoggingEntities.logRequestAndResult(systemIndexManagementRoutes) ~
    LoggingEntities.logRequestAndResult(systemGetIndexesRoutes) ~
    LoggingEntities.logRequestAndResult(languageGuesserRoutes) ~
    LoggingEntities.logRequestAndResultReduced(termRoutes) ~
    LoggingEntities.logRequestAndResultReduced(termStreamRoutes) ~
    LoggingEntities.logRequestAndResult(esTokenizersRoutes) ~
    LoggingEntities.logRequestAndResult(analyzersPlaygroundRoutes) ~
    LoggingEntities.logRequestAndResult(spellcheckRoutes) ~
    LoggingEntities.logRequestAndResult(postUserRoutes) ~
    LoggingEntities.logRequestAndResult(putUserRoutes) ~
    LoggingEntities.logRequestAndResult(getUserRoutes) ~
    LoggingEntities.logRequestAndResult(deleteUserRoutes) ~
    LoggingEntities.logRequestAndResult(genUserRoutes)
}
