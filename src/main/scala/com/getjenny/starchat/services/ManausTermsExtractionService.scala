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
                                commonOrSpecificSearch: CommonOrSpecificSearch.Value,
                                field: TermCountFields.Value) extends TokenOccurrence {

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
                                   commonOrSpecificSearch: CommonOrSpecificSearch.Value,
                                   observedDataSource: ObservedDataSources.Value,
                                   field: TermCountFields.Value) extends TokenOccurrence {

    private[this] val idxName: String = commonOrSpecificSearch match {
      case CommonOrSpecificSearch.COMMON =>
        getCommonIndexName(indexName)
      case _ => indexName
    }

    private[this] val dataService: QuestionAnswerService = observedDataSource match {
      case ObservedDataSources.KNOWLEDGEBASE =>
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
                                    minWordsPerSentence: Int,
                                    pruneTermsThreshold: Int,
                                    misspellMaxOccurrence: Int,
                                    activePotentialDecay: Int,
                                    totalInfo: Boolean,
                                    activePotential: Boolean): (List[String], Map[String, Double]) = {

    val keywordsExtraction = new KeywordsExtraction(priorOccurrences=priorOccurrences,
      observedOccurrences=observedOccurrences)

    val freqData: String = sentenceTokens.map { case(e) =>
      "word(" + e + ") -> observedOccurrences(" + observedOccurrences.tokenOccurrence(e) + ") priorOccurrences(" +
        priorOccurrences.tokenOccurrence(e) + ") totalNumberOfObservedTokens(" +
        observedOccurrences.totalNumberOfTokens + ") totalNumberOfObservedTokens(" +
        priorOccurrences.totalNumberOfTokens + ")"
    }.mkString(" ; ")

    log.debug("SentenceFrequencies: " + freqData)

    /* Informative words */
    val rawBagOfKeywordsInfo: List[(String, Double)] =
      keywordsExtraction.extractInformativeWords(sentence = sentenceTokens,
        pruneSentence = pruneTermsThreshold, minWordsPerSentence = minWordsPerSentence,
        totalInformationNorm = totalInfo)

    /* Map(keyword -> active potential) */
    val activePotentialKeywordsMap = keywordsExtraction.getWordsActivePotentialMapForSentence(rawBagOfKeywordsInfo,
      activePotentialDecay)

    val informativeKeywords: (List[String], List[(String, Double)]) = (sentenceTokens, rawBagOfKeywordsInfo)

    // list of the final keywords
    val bags: (List[String], Map[String, Double]) =
      if(activePotential) {
        keywordsExtraction.extractBagsActiveForSentence(activePotentialKeywordsMap = activePotentialKeywordsMap,
          informativeKeywords = informativeKeywords, misspellMaxOccurrence = misspellMaxOccurrence)
      } else {
        keywordsExtraction.extractBagsNoActiveForSentence(informativeKeywords = informativeKeywords,
          misspellMaxOccurrence = misspellMaxOccurrence)
      }


    log.info("ExtractedData: " + rawBagOfKeywordsInfo + " # " + activePotentialKeywordsMap +
      " # " + informativeKeywords + " # " + bags)
    println("PLN ExtractedData: " + rawBagOfKeywordsInfo + " # " + activePotentialKeywordsMap +
      " # " + informativeKeywords + " # " + bags)
    bags
  }

  def textTerms(indexName: String,
                extractionRequest: TermsExtractionRequest
               ): Map[String, Double] = {

    val priorOccurrences: TokenOccurrence = new PriorTokenOccurrenceMap(indexName = indexName,
      commonOrSpecificSearch = extractionRequest.commonOrSpecificSearchPrior.getOrElse(CommonOrSpecificSearch.COMMON),
      field = extractionRequest.fieldsPrior.getOrElse(TermCountFields.all))

    val observedOccurrences: TokenOccurrence = new ObservedTokenOccurrenceMap(indexName: String,
      commonOrSpecificSearch = extractionRequest.commonOrSpecificSearchObserved
        .getOrElse(CommonOrSpecificSearch.IDXSPECIFIC),
      observedDataSource = extractionRequest.observedDataSource.getOrElse(ObservedDataSources.KNOWLEDGEBASE),
      field = extractionRequest.fieldsObserved.getOrElse(TermCountFields.all))

    val tokenizerReq = TokenizerQueryRequest("space_punctuation", extractionRequest.text)

    val tokens: TokenizerResponse = termService.esTokenizer(indexName, tokenizerReq) match {
      case Some(t) => t
      case _ => TokenizerResponse(tokens = List.empty[TokenizerResponseItem])
    }

    log.debug("ExtractionRequest:" + extractionRequest)

    val bags = extractKeywords(sentenceTokens = tokens.tokens.map(_.token),
      observedOccurrences = observedOccurrences,
      priorOccurrences = priorOccurrences,
      minWordsPerSentence = extractionRequest.minWordsPerSentence.getOrElse(5),
      pruneTermsThreshold = extractionRequest.pruneTermsThreshold.getOrElse(100000),
      misspellMaxOccurrence = extractionRequest.misspellMaxOccurrence.getOrElse(5),
      activePotentialDecay = extractionRequest.activePotentialDecay.getOrElse(10),
      activePotential = extractionRequest.activePotential.getOrElse(true),
      totalInfo = extractionRequest.totalInfo.getOrElse(false))
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
