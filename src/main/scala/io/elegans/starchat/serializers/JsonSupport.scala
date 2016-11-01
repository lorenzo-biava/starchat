package io.elegans.starchat.serializers

/**
  * Created by Angelo Leto <angelo.leto@elegans.io> on 27/06/16.
  */

import io.elegans.starchat.entities._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json._

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val responseMessageDataFormat = jsonFormat2(ReturnMessageData)
  implicit val responseRequestUserInputFormat = jsonFormat2(ResponseRequestInUserInput)
  implicit val responseRequestInputValuesFormat = jsonFormat2(ResponseRequestInValues)
  implicit val responseRequestInputFormat = jsonFormat3(ResponseRequestIn)
  implicit val responseRequestOutputFormat = jsonFormat6(ResponseRequestOut)
  implicit val dtDocumentFormat = jsonFormat7(DTDocument)
  implicit val dtDocumentUpdateFormat = jsonFormat6(DTDocumentUpdate)
  implicit val kbDocumentFormat = jsonFormat10(KBDocument)
  implicit val kbDocumentUpdateFormat = jsonFormat9(KBDocumentUpdate)
  implicit val searchKBDocumentFormat = jsonFormat2(SearchKBDocument)
  implicit val searchDTDocumentFormat = jsonFormat2(SearchDTDocument)
  implicit val searchKBResultsFormat = jsonFormat3(SearchKBDocumentsResults)
  implicit val searchDTResultsFormat = jsonFormat3(SearchDTDocumentsResults)
  implicit val kbDocumentSearch = jsonFormat9(KBDocumentSearch)
  implicit val dtDocumentSearch = jsonFormat5(DTDocumentSearch)
  implicit val indexDocumentResultFormat = jsonFormat5(IndexDocumentResult)
  implicit val updateDocumentResultFormat = jsonFormat5(UpdateDocumentResult)
  implicit val deleteDocumentResultFormat = jsonFormat5(DeleteDocumentResult)
  implicit val listOfDocumentIdFormat = jsonFormat1(ListOfDocumentId)
}
