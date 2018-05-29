package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 23/05/18.
  */

import akka.event.{Logging, LoggingAdapter}
import com.getjenny.manaus.{KeywordsExtraction, TokenOccurrence}
import com.getjenny.starchat.SCActorSystem
import com.getjenny.starchat.entities.{TermsExtractionRequest, _}
import com.getjenny.starchat.services.esclient.ManausTermsExtractionElasticClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ManausTermsExtractionService {
  private[this] val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)
  private[this] val elasticClient: ManausTermsExtractionElasticClient.type = ManausTermsExtractionElasticClient
  private[this] val termService: TermService.type = TermService
  private[this] val priorDataService: PriorDataService.type = PriorDataService
  private[this] val convLogDataService: ConversationLogsService.type = ConversationLogsService
  private[this] val knowledgeBaseService: KnowledgeBaseService.type = KnowledgeBaseService

  /** Extract language from index name
    *
    * @param indexName the full index name
    * @return a tuple with the two component of the index (language, arbitrary pattern)
    */
  private[this] def languageFromIndex(indexName: String): (String, String) = {
    val indexLanguageRegex = "^(?:(index)_([a-z]{1,256})_([A-Za-z0-9_]{1,256}))$".r

    val (_, language, arbitrary) = indexName match {
      case indexLanguageRegex(indexPattern, languagePattern, arbitraryPattern) =>
        (indexPattern, languagePattern, arbitraryPattern)
      case _ => throw new Exception("index name is not well formed")
    }
    (language, arbitrary)
  }

  private[this] def getCommonIndexName(indexName: String): String = {
    val arbitraryPattern = elasticClient.commonIndexArbitraryPattern
    val (language, _) = languageFromIndex(indexName)
    "index_" + language + "_" + arbitraryPattern
  }

  class PriorTokenOccurrenceMap(indexName: String,
                                commonOrSpecificSearch: CommonOrSpecificSearch.Value = CommonOrSpecificSearch.COMMON,
                                field: TermCountFields.Value = TermCountFields.question) extends TokenOccurrence {

    private[this] val idxName: String = commonOrSpecificSearch match {
      case CommonOrSpecificSearch.COMMON =>
        getCommonIndexName(indexName)
      case _ => indexName
    }

    override def tokenOccurrence(word: String): Long = {
      field match {
        case TermCountFields.question =>
          val termCount = priorDataService.countTerm(indexName = idxName, field = TermCountFields.question,
            term = word)
          termCount.count
        case TermCountFields.answer =>
          val termCount = priorDataService.countTerm(indexName = idxName, field = TermCountFields.answer, term = word)
          termCount.count
        case _ =>
          val termCountQ = priorDataService.countTerm(indexName = idxName,
            field = TermCountFields.question, term = word)
          val termCountA = priorDataService.countTerm(indexName = idxName,
            field = TermCountFields.answer, term = word)
          termCountQ.count + termCountA.count
      }
    }

    override def totalNumberOfTokens: Long = {
      val numOfTerms = priorDataService.totalTerms(idxName)
      field match {
        case TermCountFields.question =>
          numOfTerms.question
        case TermCountFields.answer =>
          numOfTerms.answer
        case _ =>
          numOfTerms.question + numOfTerms.answer
      }
    }
  }

  class ObservedTokenOccurrenceMap(indexName: String,
                                   commonOrSpecificSearch: CommonOrSpecificSearch.Value = CommonOrSpecificSearch.COMMON,
                                   obsDest: ObservedSearchDests.Value = ObservedSearchDests.KNOWLEDGEBASE,
                                   field: TermCountFields.Value = TermCountFields.question) extends TokenOccurrence {

    private[this] val idxName: String = commonOrSpecificSearch match {
      case CommonOrSpecificSearch.COMMON =>
        getCommonIndexName(indexName)
      case _ => indexName
    }

    private[this] val dataService: QuestionAnswerService = obsDest match {
      case ObservedSearchDests.KNOWLEDGEBASE =>
        knowledgeBaseService
      case _ =>
        convLogDataService
    }

    override def tokenOccurrence(word: String): Long = {
      field match {
        case TermCountFields.question =>
          val termCount = dataService.countTerm(indexName = idxName, field = TermCountFields.question,
            term = word)
          termCount.count
        case TermCountFields.answer =>
          val termCount = dataService.countTerm(indexName = idxName, field = TermCountFields.answer, term = word)
          termCount.count
        case _ =>
          val termCountQ = dataService.countTerm(indexName = idxName,
            field = TermCountFields.question, term = word)
          val termCountA = dataService.countTerm(indexName = idxName,
            field = TermCountFields.answer, term = word)
          termCountQ.count + termCountA.count
      }
    }

    override def totalNumberOfTokens: Long = {
      val numOfTerms = dataService.totalTerms(idxName)
      field match {
        case TermCountFields.question =>
          numOfTerms.question
        case TermCountFields.answer =>
          numOfTerms.answer
        case _ =>
          numOfTerms.question + numOfTerms.answer
      }
    }
  }

  private[this] def extractKeywords(sentenceTokens: List[String],
                                    observedOccurrences: TokenOccurrence,
                                    priorOccurrences: TokenOccurrence,
                                    minWordsPerSentence: Int, pruneTermsThreshold: Int, misspell_max_occurrence: Int,
                                    active_potential_decay: Int,
                                    total_info: Boolean,
                                    active_potential: Boolean): (List[String], Map[String, Double]) = {

    val keywordsExtraction = new KeywordsExtraction(priorOccurrences=priorOccurrences,
      observedOccurrences=observedOccurrences)

    log.info("extract informativeWords")
    /* Informative words */
    val rawBagOfKeywordsInfo: List[(String, Double)] =
      keywordsExtraction.extractInformativeWords(sentence = sentenceTokens,
        pruneSentence = pruneTermsThreshold, minWordsPerSentence = minWordsPerSentence,
        totalInformationNorm = total_info)

    log.info("calculating active potentials Map")
    /* Map(keyword -> active potential) */
    val activePotentialKeywordsMap = keywordsExtraction.getWordsActivePotentialMapForSentence(rawBagOfKeywordsInfo,
      active_potential_decay)

    log.info("getting informative words for sentences")
    val informativeKeywords: (List[String], List[(String, Double)]) = (sentenceTokens, rawBagOfKeywordsInfo)

    log.info("calculating bags")
    // list of the final keywords
    val bags: (List[String], Map[String, Double]) =
      if(active_potential) {
        keywordsExtraction.extractBagsActiveForSentence(activePotentialKeywordsMap = activePotentialKeywordsMap,
          informativeKeywords = informativeKeywords, misspellMaxOccurrence = misspell_max_occurrence)
      } else {
        keywordsExtraction.extractBagsNoActiveForSentence(informativeKeywords = informativeKeywords,
          misspellMaxOccurrence = misspell_max_occurrence)
      }
    bags
  }

  def textTerms(indexName: String,
                extractionRequest: TermsExtractionRequest
               ): Map[String, Double] = {

    val priorOccurrences = new PriorTokenOccurrenceMap(indexName = indexName,
      commonOrSpecificSearch = extractionRequest.commonOrSpecificSearchPrior.get,
      field = extractionRequest.fieldsPrior.get)

    val observedOccurrences = new ObservedTokenOccurrenceMap(indexName: String,
      commonOrSpecificSearch = extractionRequest.commonOrSpecificSearchObserved.get,
      obsDest = extractionRequest.obsDest.get,
      field = extractionRequest.fieldsObserved.get)

    val tokenizerReq = TokenizerQueryRequest("space_punctuation", extractionRequest.text)

    val sentenceTokens: TokenizerResponse = termService.esTokenizer(indexName, tokenizerReq) match {
      case Some(t) => t
      case _ => TokenizerResponse(tokens = List.empty[TokenizerResponseItem])
    }

    val bags = this.extractKeywords(sentenceTokens = sentenceTokens.tokens.map(_.token),
      observedOccurrences = observedOccurrences,
      priorOccurrences = priorOccurrences,
      minWordsPerSentence = extractionRequest.minWordsPerSentence.get.toInt,
      pruneTermsThreshold = extractionRequest.pruneTermsThreshold.get,
      misspell_max_occurrence = extractionRequest.misspell_max_occurrence.get,
      active_potential_decay = extractionRequest.active_potential_decay.get,
      active_potential = extractionRequest.active_potential.get,
      total_info = extractionRequest.total_info.get)
    bags._2
  }

  def textTermsFuture(indexName: String,
                      extractionRequest: TermsExtractionRequest
                     ): Future[Map[String, Double]] = Future {
    textTerms(indexName = indexName, extractionRequest = extractionRequest)
  }

  def termsSynonyms(indexName: String,
                    extractionRequest: SynExtractionRequest
                   ): Map[String, Double] = {

    Map[String, Double]()
  }

  def termsSynonymsFuture(indexName: String,
                          extractionRequest: SynExtractionRequest
                         ): Future[Map[String, Double]] = Future {
    termsSynonyms(indexName = indexName, extractionRequest = extractionRequest)
  }

}
