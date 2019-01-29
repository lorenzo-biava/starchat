package com.getjenny.starchat.resources

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.pattern.CircuitBreaker
import com.getjenny.starchat.entities.{NodeDtLoadingStatus, Permissions, ReturnMessageData}
import com.getjenny.starchat.routing.{StarChatCircuitBreaker, StarChatResource}
import com.getjenny.starchat.services.{ClusterNodesService, NodeDtLoadingStatusService}

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 29/01/19.
  */

trait NodeDtLoadingStatusResource extends StarChatResource {
  private[this] val nodeDtLoadingStatusService: NodeDtLoadingStatusService.type = NodeDtLoadingStatusService
  private[this] val routeName: String = """node_dt_update"""

  def nodeDtLoadingStatusRoutes: Route = handleExceptions(routesExceptionHandler) {
    pathPrefix(routeName) {
      get {
        path(Segment) { indexName: String =>
          authenticateBasicAsync(realm = authRealm,
            authenticator = authenticator.authenticator) { user =>
            authorizeAsync(_ =>
              authenticator.hasPermissions(user, "admin", Permissions.read)) {
              extractMethod { method =>
                val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
                onCompleteWithBreaker(breaker)(Future {
                  nodeDtLoadingStatusService.loadingStatus(indexName)
                }) {
                  case Success(t) =>
                    completeResponse(StatusCodes.OK, StatusCodes.BadRequest, t)
                  case Failure(e) =>
                    log.error("DtUpdate(" + indexName + ") route=nodeDtLoadingStatusRoutes method=" + method +" : " + e.getMessage)
                    completeResponse(StatusCodes.BadRequest,
                      Option {
                        ReturnMessageData(code = 100, message = e.getMessage)
                      })
                }
              }
            }
          }
        }
      } ~
        put {
          path(Segment) { uuid: String =>
            authenticateBasicAsync(realm = authRealm,
              authenticator = authenticator.authenticator) { user =>
              authorizeAsync(_ =>
                authenticator.hasPermissions(user, "admin", Permissions.read)) {
                extractMethod { method =>
                  entity(as[NodeDtLoadingStatus]) { document =>
                    val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
                    onCompleteWithBreaker(breaker)(Future {
                      nodeDtLoadingStatusService.update(document)
                    }) {
                      case Success(_) =>
                        completeResponse(StatusCodes.OK)
                      case Failure(e) =>
                        log.error("DtUpdate(" + uuid + ") route=nodeDtLoadingStatusRoutes method=" + method + " : " + e.getMessage)
                        completeResponse(StatusCodes.BadRequest,
                          Option {
                            ReturnMessageData(code = 101, message = e.getMessage)
                          })
                    }
                  }
                }
              }
            }
          }
        } ~
        delete {
          path(Segment) { uuid: String =>
            authenticateBasicAsync(realm = authRealm,
              authenticator = authenticator.authenticator) { user =>
              authorizeAsync(_ =>
                authenticator.hasPermissions(user, "admin", Permissions.read)) {
                extractMethod { method =>
                  val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
                  onCompleteWithBreaker(breaker)(Future {
                    nodeDtLoadingStatusService.cleanDeadNodesRecords
                  }) {
                    case Success(t) =>
                      completeResponse(StatusCodes.OK, StatusCodes.BadRequest, t)
                    case Failure(e) =>
                      log.error("DtUpdate(" + uuid + ") route=nodeDtLoadingStatusRoutes method=" + method + " : " + e.getMessage)
                      completeResponse(StatusCodes.BadRequest,
                        Option {
                          ReturnMessageData(code = 102, message = e.getMessage)
                        })
                  }
                }
              }
            }
          }
        }
    }
  }
}
