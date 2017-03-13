package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 10/03/17.
  */

import com.getjenny.starchat.entities.{LanguageGuesserRequestOut, LanguageGuesserRequestIn, LanguageGuesserInformations}

import scala.concurrent.{ExecutionContext}

import org.apache.tika.langdetect.OptimaizeLangDetector
import org.apache.tika.language.detect.LanguageDetector
import org.apache.tika.language.detect.LanguageResult

/**
  * Implements functions, eventually used by LanguageGuesserResource
  */
class LanguageGuesserService(implicit val executionContext: ExecutionContext) {

  def guess_language(request_data: LanguageGuesserRequestIn) : Option[LanguageGuesserRequestOut] = {
    val detector: LanguageDetector = new OptimaizeLangDetector().loadModels()
    val result: LanguageResult = detector.detect(request_data.input_text)

    Option {
      LanguageGuesserRequestOut(result.getLanguage, result.getRawScore,
        result.getConfidence.name,
        detector.hasEnoughText
      )
    }
  }

  def get_languages(language_code: String/*ISO 639-1 name for language*/):
      Option[LanguageGuesserInformations] = {
    val detector: LanguageDetector = new OptimaizeLangDetector().loadModels()
    val has_model: Boolean = detector.hasModel(language_code)
    Option {
      LanguageGuesserInformations(
        Map[String,Map[String,Boolean]](
        "languages" -> Map[String, Boolean](language_code -> has_model))
      )
    }
  }
}
