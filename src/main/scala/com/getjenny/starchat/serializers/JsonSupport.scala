package com.getjenny.starchat.serializers

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 27/06/16.
  */

import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import com.getjenny.analyzer.expressions.Data
import com.getjenny.starchat.entities._
import spray.json._
import akka.http.scaladsl.unmarshalling.Unmarshaller

import scalaz.Scalaz._

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  private[this] val start: ByteString = ByteString.empty
  private[this] val sep: ByteString = ByteString("\n")
  private[this] val end: ByteString = ByteString.empty

  implicit val jsonStreamingSupport: JsonEntityStreamingSupport =
    EntityStreamingSupport.json(1024 * 1024) // max object size 1MB
      .withFramingRendererFlow(Flow[ByteString].intersperse(start, sep, end).asJava)
      .withParallelMarshalling(parallelism = 8, unordered = true)

  implicit val responseMessageDataFormat = jsonFormat2(ReturnMessageData)
  implicit val responseRequestUserInputFormat = jsonFormat2(ResponseRequestInUserInput)
  implicit val responseRequestInputValuesFormat = jsonFormat2(ResponseRequestInValues)
  implicit val responseRequestInputFormat = jsonFormat6(ResponseRequestIn)
  implicit val responseRequestOutputFormat = jsonFormat13(ResponseRequestOut)
  implicit val dtDocumentFormat = jsonFormat12(DTDocument)
  implicit val dtDocumentUpdateFormat = jsonFormat10(DTDocumentUpdate)
  implicit val kbDocumentFormat = jsonFormat14(KBDocument)
  implicit val kbDocumentUpdateFormat = jsonFormat13(KBDocumentUpdate)
  implicit val searchKBDocumentFormat = jsonFormat2(SearchKBDocument)
  implicit val searchDTDocumentFormat = jsonFormat2(SearchDTDocument)
  implicit val searchKBResultsFormat = jsonFormat3(SearchKBDocumentsResults)
  implicit val searchDTResultsFormat = jsonFormat3(SearchDTDocumentsResults)
  implicit val kbDocumentSearchFormat = jsonFormat16(KBDocumentSearch)
  implicit val dtDocumentSearchFormat = jsonFormat7(DTDocumentSearch)
  implicit val indexDocumentResultFormat = jsonFormat5(IndexDocumentResult)
  implicit val updateDocumentResultFormat = jsonFormat5(UpdateDocumentResult)
  implicit val deleteDocumentResultFormat = jsonFormat5(DeleteDocumentResult)
  implicit val indexDocumentResultListFormat = jsonFormat1(IndexDocumentListResult)
  implicit val updateDocumentResultListFormat = jsonFormat1(UpdateDocumentListResult)
  implicit val deleteDocumentResultListFormat = jsonFormat1(DeleteDocumentListResult)
  implicit val deleteDocumentsResultFormat = jsonFormat2(DeleteDocumentsResult)
  implicit val listOfDocumentIdFormat = jsonFormat1(ListOfDocumentId)
  implicit val dtAnalyzerItemFormat = jsonFormat3(DTAnalyzerItem)
  implicit val dtAnalyzerMapFormat = jsonFormat1(DTAnalyzerMap)
  implicit val dtAnalyzerLoadFormat = jsonFormat1(DTAnalyzerLoad)
  implicit val indexManagementResponseFormat = jsonFormat1(IndexManagementResponse)
  implicit val languageGuesserRequestInFormat = jsonFormat1(LanguageGuesserRequestIn)
  implicit val languageGuesserRequestOutFormat = jsonFormat4(LanguageGuesserRequestOut)
  implicit val languageGuesserInformationsFormat = jsonFormat1(LanguageGuesserInformations)
  implicit val searchTermFormat = jsonFormat10(SearchTerm)
  implicit val termFormat = jsonFormat9(Term)
  implicit val termIdsRequestFormat = jsonFormat1(TermIdsRequest)
  implicit val termsFormat = jsonFormat1(Terms)
  implicit val termsResultsFormat = jsonFormat3(TermsResults)
  implicit val textTermsFormat = jsonFormat4(TextTerms)
  implicit val failedShardsFormat = jsonFormat4(FailedShard)
  implicit val refreshIndexResultFormat = jsonFormat5(RefreshIndexResult)
  implicit val refreshIndexResultsFormat = jsonFormat1(RefreshIndexResults)
  implicit val analyzerQueryRequestFormat = jsonFormat2(TokenizerQueryRequest)
  implicit val analyzerResponseItemFormat = jsonFormat5(TokenizerResponseItem)
  implicit val analyzerResponseFormat = jsonFormat1(TokenizerResponse)
  implicit val analyzerDataFormat = jsonFormat2(Data)
  implicit val analyzerEvaluateRequestFormat = jsonFormat3(AnalyzerEvaluateRequest)
  implicit val analyzerEvaluateResponseFormat = jsonFormat4(AnalyzerEvaluateResponse)
  implicit val spellcheckTokenSuggestionsFormat = jsonFormat3(SpellcheckTokenSuggestions)
  implicit val spellcheckTokenFormat = jsonFormat4(SpellcheckToken)
  implicit val spellcheckTermsResponseFormat = jsonFormat1(SpellcheckTermsResponse)
  implicit val spellcheckTermsRequestFormat = jsonFormat4(SpellcheckTermsRequest)
  implicit val responseRequestOutOperationResultFormat = jsonFormat2(ResponseRequestOutOperationResult)

  implicit object PermissionsJsonFormat extends JsonFormat[Permissions.Value] {
    def write(obj: Permissions.Value): JsValue = JsString(obj.toString)
    def read(json: JsValue): Permissions.Value = json match {
      case JsString(str) =>
        Permissions.values.find(_.toString === str) match {
          case Some(t) => t
          case _ => throw DeserializationException("Permission string is invalid")
        }
      case _ => throw DeserializationException("Permission string expected")
    }
  }

  implicit val userFormat = jsonFormat4(User)
  implicit val userUpdateFormat = jsonFormat3(UserUpdate)
  implicit val dtReloadTimestamp = jsonFormat2(DtReloadTimestamp)
  implicit val openCloseIndexFormat = jsonFormat5(OpenCloseIndex)

  implicit val commonOrSpecificSearchUnmarshalling:
    Unmarshaller[String, CommonOrSpecificSearch.Value] =
    Unmarshaller.strict[String, CommonOrSpecificSearch.Value] { enumValue =>
      CommonOrSpecificSearch.value(enumValue)
    }

  implicit object commonOrSpecificSearchFormat extends JsonFormat[CommonOrSpecificSearch.Value] {
    def write(obj: CommonOrSpecificSearch.Value): JsValue = JsString(obj.toString)
    def read(json: JsValue): CommonOrSpecificSearch.Value = json match {
      case JsString(str) =>
        CommonOrSpecificSearch.values.find(_.toString === str) match {
          case Some(t) => t
          case _ => throw DeserializationException("CommonOrSpecificSearch string is invalid")
        }
      case _ => throw DeserializationException("CommonOrSpecificSearch string expected")
    }
  }

  implicit val observedSearchDestUnmarshalling:
    Unmarshaller[String, ObservedSearchDests.Value] = Unmarshaller.strict[String, ObservedSearchDests.Value] { enumValue =>
    ObservedSearchDests.value(enumValue)
  }

  implicit object observedSearchDestFormat extends JsonFormat[ObservedSearchDests.Value] {
    def write(obj: ObservedSearchDests.Value): JsValue = JsString(obj.toString)
    def read(json: JsValue): ObservedSearchDests.Value = json match {
      case JsString(str) =>
        ObservedSearchDests.values.find(_.toString === str) match {
          case Some(t) => t
          case _ => throw DeserializationException("ObservedSearchDests string is invalid")
        }
      case _ => throw DeserializationException("ObservedSearchDests string expected")
    }
  }

  implicit val termCountFieldsUnmarshalling:
    Unmarshaller[String, TermCountFields.Value] =
    Unmarshaller.strict[String, TermCountFields.Value] { enumValue =>
      TermCountFields.value(enumValue)
    }

  implicit object termCountFieldsFormat extends JsonFormat[TermCountFields.Value] {
    def write(obj: TermCountFields.Value): JsValue = JsString(obj.toString)
    def read(json: JsValue): TermCountFields.Value = json match {
      case JsString(str) =>
        TermCountFields.values.find(_.toString === str) match {
          case Some(t) => t
          case _ => throw DeserializationException("TermCountFields string is invalid")
        }
      case _ => throw DeserializationException("TermCountFields string expected")
    }
  }

  implicit val termCountFormat = jsonFormat2(TermCount)
  implicit val totalTermsFormat = jsonFormat3(TotalTerms)
  implicit val dictSizeFormat = jsonFormat4(DictSize)
  implicit val termsExtractionRequestFormat = jsonFormat12(TermsExtractionRequest)
  implicit val synExtractionRequest = jsonFormat13(SynExtractionRequest)
}
