package com.getjenny.starchat.resources

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 19/12/16.
  */

import akka.http.scaladsl.server.Route
import com.getjenny.starchat.entities._
import com.getjenny.starchat.routing.MyResource
import scala.concurrent.{Future}
import akka.http.scaladsl.model.StatusCodes

trait RootAPIResource extends MyResource {
  def rootAPIsRoutes: Route = pathPrefix("") {
    pathEnd {
      get {
        val result: Option[RootAPIsDescription] = Option(new RootAPIsDescription)
        completeResponse(StatusCodes.OK, StatusCodes.BadRequest, result)
      }
    }
  }
}



