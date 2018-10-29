package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 10/03/17.
  */

import akka.event.{Logging, LoggingAdapter}
import com.getjenny.starchat.SCActorSystem
import com.getjenny.starchat.entities.{LanguageGuesserInformations, LanguageGuesserRequestIn, LanguageGuesserRequestOut}
import org.apache.tika.langdetect.OptimaizeLangDetector
import org.apache.tika.language.detect.{LanguageDetector, LanguageResult}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Implements functions, eventually used by LanguageGuesserResource
  */
object LanguageGuesserService {

  def guessLanguage(indexName: String, requestData: LanguageGuesserRequestIn):
  Future[LanguageGuesserRequestOut] = Future {
    val detector: LanguageDetector = new OptimaizeLangDetector().loadModels()
    val result: LanguageResult = detector.detect(requestData.input_text)

    LanguageGuesserRequestOut(result.getLanguage, result.getRawScore,
      result.getConfidence.name,
      detector.hasEnoughText
    )
  }

  def getLanguages(indexName: String, languageCode: String /*ISO 639-1 name for language*/):
  Future[LanguageGuesserInformations] = Future {
    val detector: LanguageDetector = new OptimaizeLangDetector().loadModels()
    val hasModel: Boolean = detector.hasModel(languageCode)
    LanguageGuesserInformations(
      Map[String,Map[String,Boolean]](
        "languages" -> Map[String, Boolean](languageCode -> hasModel))
    )
  }
}
