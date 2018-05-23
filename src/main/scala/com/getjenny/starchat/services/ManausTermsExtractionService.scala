package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 23/05/18.
  */

import akka.event.{Logging, LoggingAdapter}
import com.getjenny.starchat.SCActorSystem
import com.getjenny.manaus.KeywordsExtraction
import com.getjenny.starchat.entities.{SearchTerm, StatSearchModes, TermSearchModes}

object ManausTermsExtractionService {
  private[this] val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)
  private[this] val knowledgeBaseService: KnowledgeBaseService.type = KnowledgeBaseService
  private[this] val termService: TermService.type = TermService
  private[this] val statTextService: StatTextService.type = StatTextService

  /*
  def textTerms(indexName: String, text: String,
                termSearchMode: TermSearchModes.Value = TermSearchModes.TERMS_COMMON,
                statSearchMode: StatSearchModes.Value =
                  StatSearchModes.STAT_COMMON_KNOWLEDGEBASE): Map[String, Double] = {

    //1- extract terms through manaus using:
    // 1a- the knowledge base for conversation logs
    // 1b- the StatText service resource (with subtitles)
  }

  def termsSynonyms(indexName: String, text: String,
                    termSearchMode: TermSearchModes.Value = TermSearchModes.TERMS_COMMON,
                    statSearchMode: StatSearchModes.Value =
                    StatSearchModes.STAT_COMMON_KNOWLEDGEBASE): Map[String, Double] = {
  }
  */

}
