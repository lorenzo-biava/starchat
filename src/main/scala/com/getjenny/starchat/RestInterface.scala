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
  with TermResource with TokenizersResource with AnalyzersPlaygroundResource
  with SpellcheckResource with UserResource with ConversationLogsResource with StatConversationsResource {

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
  lazy val statConversationsService = StatConversationsService

  val routes: Route = rootAPIsRoutes ~
    LoggingEntities.logRequestAndResultReduced(super[KnowledgeBaseResource].questionAnswerRoutes) ~
    LoggingEntities.logRequestAndResultReduced(super[KnowledgeBaseResource].questionAnswerStreamRoutes) ~
    LoggingEntities.logRequestAndResult(super[KnowledgeBaseResource].questionAnswerSearchRoutes) ~
    LoggingEntities.logRequestAndResult(super[KnowledgeBaseResource].totalTermsRoutes) ~
    LoggingEntities.logRequestAndResult(super[KnowledgeBaseResource].dictSizeRoutes) ~
    LoggingEntities.logRequestAndResult(super[KnowledgeBaseResource].termsCountRoutes) ~
    LoggingEntities.logRequestAndResultReduced(super[ConversationLogsResource].questionAnswerRoutes) ~
    LoggingEntities.logRequestAndResultReduced(super[ConversationLogsResource].questionAnswerStreamRoutes) ~
    LoggingEntities.logRequestAndResult(super[ConversationLogsResource].questionAnswerSearchRoutes) ~
    LoggingEntities.logRequestAndResult(super[ConversationLogsResource].totalTermsRoutes) ~
    LoggingEntities.logRequestAndResult(super[ConversationLogsResource].dictSizeRoutes) ~
    LoggingEntities.logRequestAndResult(super[ConversationLogsResource].termsCountRoutes) ~
    LoggingEntities.logRequestAndResultReduced(super[StatConversationsResource].questionAnswerRoutes) ~
    LoggingEntities.logRequestAndResultReduced(super[StatConversationsResource].questionAnswerStreamRoutes) ~
    LoggingEntities.logRequestAndResult(super[StatConversationsResource].questionAnswerSearchRoutes) ~
    LoggingEntities.logRequestAndResult(super[StatConversationsResource].totalTermsRoutes) ~
    LoggingEntities.logRequestAndResult(super[StatConversationsResource].dictSizeRoutes) ~
    LoggingEntities.logRequestAndResult(super[StatConversationsResource].termsCountRoutes) ~
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
