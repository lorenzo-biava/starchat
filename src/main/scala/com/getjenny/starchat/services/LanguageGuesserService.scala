package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 10/03/17.
  */

import akka.event.{Logging, LoggingAdapter}
import com.getjenny.starchat.SCActorSystem
import com.getjenny.starchat.entities.{LanguageGuesserInformations, LanguageGuesserRequestIn, LanguageGuesserRequestOut}

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext
import org.apache.tika.langdetect.OptimaizeLangDetector
import org.apache.tika.language.detect.LanguageDetector
import org.apache.tika.language.detect.LanguageResult
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Implements functions, eventually used by LanguageGuesserResource
  */
object LanguageGuesserService {
  val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)
  def guessLanguage(indexName: String, requestData: LanguageGuesserRequestIn):
  Future[Option[LanguageGuesserRequestOut]] = Future {
    val detector: LanguageDetector = new OptimaizeLangDetector().loadModels()
    val result: LanguageResult = detector.detect(requestData.input_text)

    Option {
      LanguageGuesserRequestOut(result.getLanguage, result.getRawScore,
        result.getConfidence.name,
        detector.hasEnoughText
      )
    }
  }

  def getLanguages(indexName: String, languageCode: String /*ISO 639-1 name for language*/):
      Future[Option[LanguageGuesserInformations]] = Future {
    val detector: LanguageDetector = new OptimaizeLangDetector().loadModels()
    val hasModel: Boolean = detector.hasModel(languageCode)
    Option {
      LanguageGuesserInformations(
        Map[String,Map[String,Boolean]](
        "languages" -> Map[String, Boolean](languageCode -> hasModel))
      )
    }
  }
}
