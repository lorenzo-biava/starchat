package com.getjenny.starchat.serializers

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 27/06/16.
  */

import com.getjenny.starchat.entities._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCode
import spray.json._

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val responseAPIsDescriptionDataFormat = jsonFormat1(RootAPIsDescription)
  implicit val responseMessageDataFormat = jsonFormat2(ReturnMessageData)
  implicit val responseRequestUserInputFormat = jsonFormat2(ResponseRequestInUserInput)
  implicit val responseRequestInputValuesFormat = jsonFormat2(ResponseRequestInValues)
  implicit val responseRequestInputFormat = jsonFormat6(ResponseRequestIn)
  implicit val responseRequestOutputFormat = jsonFormat13(ResponseRequestOut)
  implicit val dtDocumentFormat = jsonFormat11(DTDocument)
  implicit val dtDocumentUpdateFormat = jsonFormat10(DTDocumentUpdate)
  implicit val kbDocumentFormat = jsonFormat12(KBDocument)
  implicit val kbDocumentUpdateFormat = jsonFormat11(KBDocumentUpdate)
  implicit val searchKBDocumentFormat = jsonFormat2(SearchKBDocument)
  implicit val searchDTDocumentFormat = jsonFormat2(SearchDTDocument)
  implicit val searchKBResultsFormat = jsonFormat3(SearchKBDocumentsResults)
  implicit val searchDTResultsFormat = jsonFormat3(SearchDTDocumentsResults)
  implicit val kbDocumentSearchFormat = jsonFormat11(KBDocumentSearch)
  implicit val dtDocumentSearchFormat = jsonFormat7(DTDocumentSearch)
  implicit val indexDocumentResultFormat = jsonFormat5(IndexDocumentResult)
  implicit val updateDocumentResultFormat = jsonFormat5(UpdateDocumentResult)
  implicit val deleteDocumentResultFormat = jsonFormat5(DeleteDocumentResult)
  implicit val indexDocumentResultListFormat = jsonFormat1(IndexDocumentListResult)
  implicit val updateDocumentResultListFormat = jsonFormat1(UpdateDocumentListResult)
  implicit val deleteDocumentResultListFormat = jsonFormat1(DeleteDocumentListResult)
  implicit val listOfDocumentIdFormat = jsonFormat1(ListOfDocumentId)
  implicit val dtAnalyzerItem = jsonFormat3(DTAnalyzerItem)
  implicit val dtAnalyzerMapFormat = jsonFormat1(DTAnalyzerMap)
  implicit val dtAnalyzerLoadFormat = jsonFormat1(DTAnalyzerLoad)
  implicit val indexManagementResponseFormat = jsonFormat1(IndexManagementResponse)
  implicit val languageGuesserRequestInFormat = jsonFormat1(LanguageGuesserRequestIn)
  implicit val languageGuesserRequestOuFormatt = jsonFormat4(LanguageGuesserRequestOut)
  implicit val languageGuesserInformationsFormat = jsonFormat1(LanguageGuesserInformations)
  implicit val termFormat = jsonFormat9(Term)
  implicit val termIdsRequestFormat = jsonFormat1(TermIdsRequest)
  implicit val termsFormat = jsonFormat1(Terms)
  implicit val termsResultsFormat = jsonFormat3(TermsResults)
  implicit val textTerms = jsonFormat4(TextTerms)
  implicit val failedShards = jsonFormat4(FailedShard)
  implicit val refreshIndexResult = jsonFormat4(RefreshIndexResult)
  implicit val analyzerQueryRequest = jsonFormat2(TokenizerQueryRequest)
  implicit val analyzerResponseItem = jsonFormat5(TokenizerResponseItem)
  implicit val analyzerResponse = jsonFormat1(TokenizerResponse)
  implicit val analyzerEvaluateRequest = jsonFormat2(AnalyzerEvaluateRequest)
  implicit val analyzerEvaluateResponse = jsonFormat4(AnalyzerEvaluateResponse)
  implicit val spellcheckTokenSuggestions = jsonFormat3(SpellcheckTokenSuggestions)
  implicit val spellcheckToken = jsonFormat4(SpellcheckToken)
  implicit val spellcheckTermsResponse = jsonFormat1(SpellcheckTermsResponse)
  implicit val spellcheckTermsRequest = jsonFormat3(SpellcheckTermsRequest)
  implicit val responseRequestOutOperationResult = jsonFormat2(ResponseRequestOutOperationResult)
}
