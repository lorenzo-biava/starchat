package com.getjenny.starchat.resources

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 27/06/16.
  */

import akka.http.scaladsl.server.Route
import com.getjenny.starchat.entities._
import com.getjenny.starchat.routing.MyResource
import com.getjenny.starchat.services.DecisionTableService
import akka.http.scaladsl.model.StatusCodes

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

trait DecisionTableResource extends MyResource {

  val dtElasticService: DecisionTableService

  def decisionTableRoutes: Route = pathPrefix("decisiontable") {
    pathEnd {
      post {
        entity(as[DTDocument]) { document =>
          val result: Future[Option[IndexDocumentResult]] = dtElasticService.create(document)
          onSuccess(result) {
            case Some(v) =>
              completeIndexDocumentJson(201, 410, Future{Option{v}})
            case None =>
              completeResponseMessageData(400,
                Future{Option{ReturnMessageData(code = 300, message = "Error indexing new document")}})
          }
        }
      } ~
        get {
          parameters("ids".as[String].*) { ids =>
            val result: Future[Option[SearchDTDocumentsResults]] = dtElasticService.read(ids.toList)
            completeSearchDTDocumentResultsJson(200, 400, result)
          }
        }
    } ~
      path(Segment) { id =>
        put {
          entity(as[DTDocumentUpdate]) { update =>
            val result: Future[Option[UpdateDocumentResult]] = dtElasticService.update(id, update)
            val result_try: Try[Option[UpdateDocumentResult]] = Await.ready(result,  30 seconds).value.get
            result_try match {
              case Success(t) =>
                completeUpdateDocumentResultJson(201, 400, result)
              case Failure(e) =>
                completeResponseMessageData(400,
                  Future{Option{ReturnMessageData(code = 101, message = e.getMessage)}})
            }
          }
        } ~
          delete {
            val result: Future[Option[DeleteDocumentResult]] = dtElasticService.delete(id)
            completeDeleteDocumentResultJson(200, 400, result)
          }
      }
  }

  def decisionTableSearchRoutes: Route = pathPrefix("decisiontable_search") {
    pathEnd {
      post {
        entity(as[DTDocumentSearch]) { docsearch =>
          val result: Future[Option[SearchDTDocumentsResults]] = dtElasticService.search(docsearch)
          completeSearchDTDocumentResultsJson(200, 400, result)
        }
      }
    }
  }


  def decisionTableResponseRequestRoutes: Route = pathPrefix("get_next_response") {
    pathEnd {
      post {
        entity(as[ResponseRequestIn]) { response_request =>
          val response: Option[ResponseRequestOutOperationResult] =
            dtElasticService.getNextResponse(response_request)
          response match {
            case Some(t) =>
              if (t.status.code == 200) {
                completeResponseRequestOutJson (t.status.code, 410, t.response_request_out)
              }  else {
                complete(t.status.code, None) // no response found
              }
            case None => completeResponseRequestOutJson (200, 410, null)
          }
        }
      }
    }
  }

}



