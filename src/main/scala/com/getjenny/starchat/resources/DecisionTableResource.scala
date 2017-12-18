package com.getjenny.starchat.resources

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 27/06/16.
  */

import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.server.Route
import com.getjenny.starchat.entities._
import com.getjenny.starchat.routing._
import com.getjenny.starchat.services.{AnalyzerService, DecisionTableService, ResponseService}
import akka.http.scaladsl.model.StatusCodes
import akka.pattern.CircuitBreaker
import com.getjenny.starchat.SCActorSystem
import com.getjenny.analyzer.analyzers._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

trait DecisionTableResource extends MyResource {

  def decisionTableRoutes: Route =
    pathPrefix("""^(index_(?:[A-Za-z0-9_]+))$""".r ~ Slash ~ "decisiontable") { index_name =>
      pathEnd {
        val decisionTableService = DecisionTableService
        post {
          authenticateBasicAsync(realm = auth_realm,
            authenticator = authenticator.authenticator) { user =>
            authorizeAsync(_ =>
              authenticator.hasPermissions(user, index_name, Permissions.write)) {
              parameters("refresh".as[Int] ? 0) { refresh =>
                entity(as[DTDocument]) { document =>
                  val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
                  onCompleteWithBreaker(breaker)(decisionTableService.create(index_name, document, refresh)) {
                    case Success(t) =>
                      completeResponse(StatusCodes.Created, StatusCodes.BadRequest, t)
                    case Failure(e) =>
                      log.error("index(" + index_name + ") route=decisionTableRoutes method=POST: " + e.getMessage)
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
          get {
            authenticateBasicAsync(realm = auth_realm,
              authenticator = authenticator.authenticator) { user =>
              authorizeAsync(_ =>
                authenticator.hasPermissions(user, index_name, Permissions.read)) {
                parameters("ids".as[String].*, "dump".as[Boolean] ? false) { (ids, dump) =>
                  if (!dump) {
                    val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
                    onCompleteWithBreaker(breaker)(decisionTableService.read(index_name, ids.toList)) {
                      case Success(t) =>
                        completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option {
                          t
                        })
                      case Failure(e) =>
                        log.error("index(" + index_name + ") route=decisionTableRoutes method=GET: " + e.getMessage)
                        completeResponse(StatusCodes.BadRequest,
                          Option {
                            ReturnMessageData(code = 101, message = e.getMessage)
                          })
                    }
                  } else {
                    val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
                    onCompleteWithBreaker(breaker)(decisionTableService.getDTDocuments(index_name)) {
                      case Success(t) =>
                        completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option {
                          t
                        })
                      case Failure(e) =>
                        log.error("index(" + index_name + ") route=decisionTableRoutes method=GET: " + e.getMessage)
                        completeResponse(StatusCodes.BadRequest,
                          Option {
                            ReturnMessageData(code = 102, message = e.getMessage)
                          })
                    }
                  }
                }
              }
            }
          } ~
          delete {
            authenticateBasicAsync(realm = auth_realm,
              authenticator = authenticator.authenticator) { user =>
              authorizeAsync(_ =>
                authenticator.hasPermissions(user, index_name, Permissions.write)) {
                val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
                onCompleteWithBreaker(breaker)(decisionTableService.deleteAll(index_name)) {
                  case Success(t) =>
                    completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option {
                      t
                    })
                  case Failure(e) =>
                    log.error("index(" + index_name + ") route=decisionTableRoutes method=DELETE : " + e.getMessage)
                    completeResponse(StatusCodes.BadRequest,
                      Option {
                        ReturnMessageData(code = 103, message = e.getMessage)
                      })
                }
              }
            }
          }
      } ~
        path(Segment) { id =>
          put {
            authenticateBasicAsync(realm = auth_realm,
              authenticator = authenticator.authenticator) { user =>
              authorizeAsync(_ =>
                authenticator.hasPermissions(user, index_name, Permissions.write)) {
                entity(as[DTDocumentUpdate]) { update =>
                  parameters("refresh".as[Int] ? 0) { refresh =>
                    val decisionTableService = DecisionTableService
                    val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
                    onCompleteWithBreaker(breaker)(decisionTableService.update(index_name, id, update, refresh)) {
                      case Success(t) =>
                        completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option {
                          t
                        })
                      case Failure(e) =>
                        log.error("index(" + index_name + ") route=decisionTableRoutes method=PUT : " + e.getMessage)
                        completeResponse(StatusCodes.BadRequest,
                          Option {
                            ReturnMessageData(code = 104, message = e.getMessage)
                          })
                    }
                  }
                }
              }
            }
          } ~
            delete {
              authenticateBasicAsync(realm = auth_realm,
                authenticator = authenticator.authenticator) { user =>
                authorizeAsync(_ =>
                  authenticator.hasPermissions(user, index_name, Permissions.write)) {
                  parameters("refresh".as[Int] ? 0) { refresh =>
                    val decisionTableService = DecisionTableService
                    val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
                    onCompleteWithBreaker(breaker)(decisionTableService.delete(index_name, id, refresh)) {
                      case Success(t) =>
                        if (t.isDefined) {
                          completeResponse(StatusCodes.OK, t)
                        } else {
                          completeResponse(StatusCodes.BadRequest, t)
                        }
                      case Failure(e) =>
                        log.error("index(" + index_name + ") route=decisionTableRoutes method=DELETE : " + e.getMessage)
                        completeResponse(StatusCodes.BadRequest,
                          Option {
                            ReturnMessageData(code = 105, message = e.getMessage)
                          })
                    }
                  }
                }
              }
            }
        }
    }

  def decisionTableUploadCSVRoutes: Route =
    pathPrefix("""^(index_(?:[A-Za-z0-9_]+))$""".r ~ Slash ~ "decisiontable_upload_csv") { index_name =>
      pathEnd {
        authenticateBasicAsync(realm = auth_realm,
          authenticator = authenticator.authenticator) { user =>
          authorizeAsync(_ =>
            authenticator.hasPermissions(user, index_name, Permissions.write)) {
            uploadedFile("csv") {
              case (metadata, file) =>
                val decisionTableService = DecisionTableService
                val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker(callTimeout = 10.seconds)
                onCompleteWithBreaker(breaker)(decisionTableService.indexCSVFileIntoDecisionTable(index_name, file)) {
                  case Success(t) =>
                    file.delete()
                    completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option {
                      t
                    })
                  case Failure(e) =>
                    log.error("index(" + index_name + ") route=decisionTableUploadCSVRoutes method=POST: " + e.getMessage)
                    completeResponse(StatusCodes.BadRequest,
                      Option {
                        ReturnMessageData(code = 107, message = e.getMessage)
                      })
                }
            }
          }
        }
      }
    }

  def decisionTableAnalyzerRoutes: Route =
    pathPrefix("""^(index_(?:[A-Za-z0-9_]+))$""".r ~ Slash ~ "decisiontable_analyzer") { index_name =>
      pathEnd {
        get {
          authenticateBasicAsync(realm = auth_realm,
            authenticator = authenticator.authenticator) { user =>
            authorizeAsync(_ =>
              authenticator.hasPermissions(user, index_name, Permissions.read)) {
              val analyzerService = AnalyzerService
              val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
              onCompleteWithBreaker(breaker)(analyzerService.getDTAnalyzerMap(index_name)) {
                case Success(t) =>
                  completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option {
                    t
                  })
                case Failure(e) =>
                  log.error("index(" + index_name + ") route=decisionTableAnalyzerRoutes method=GET: " + e.getMessage)
                  completeResponse(StatusCodes.BadRequest,
                    Option {
                      ReturnMessageData(code = 106, message = e.getMessage)
                    })
              }
            }
          }
        } ~
          post {
            authenticateBasicAsync(realm = auth_realm,
              authenticator = authenticator.authenticator) { user =>
              authorizeAsync(_ =>
                authenticator.hasPermissions(user, index_name, Permissions.write)) {
                val analyzerService = AnalyzerService
                val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
                onCompleteWithBreaker(breaker)(analyzerService.loadAnalyzer(index_name, propagate = true)) {
                  case Success(t) =>
                    completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option {
                      t
                    })
                  case Failure(e) =>
                    log.error("index(" + index_name + ") route=decisionTableAnalyzerRoutes method=POST: " + e.getMessage)
                    completeResponse(StatusCodes.BadRequest,
                      Option {
                        ReturnMessageData(code = 107, message = e.getMessage)
                      })
                }
              }
            }
          }
      }
    }

  def decisionTableSearchRoutes: Route =
    pathPrefix("""^(index_(?:[A-Za-z0-9_]+))$""".r ~ Slash ~ "decisiontable_search") { index_name =>
      pathEnd {
        post {
          authenticateBasicAsync(realm = auth_realm,
            authenticator = authenticator.authenticator) { user =>
            authorizeAsync(_ =>
              authenticator.hasPermissions(user, index_name, Permissions.read)) {
              entity(as[DTDocumentSearch]) { docsearch =>
                val decisionTableService = DecisionTableService
                val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
                onCompleteWithBreaker(breaker)(decisionTableService.search(index_name, docsearch)) {
                  case Success(t) =>
                    completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option {
                      t
                    })
                  case Failure(e) =>
                    log.error("index(" + index_name + ") route=decisionTableSearchRoutes method=POST: " + e.getMessage)
                    completeResponse(StatusCodes.BadRequest,
                      Option {
                        ReturnMessageData(code = 108, message = e.getMessage)
                      })
                }
              }
            }
          }
        }
      }
    }

  def decisionTableResponseRequestRoutes: Route =
    pathPrefix("""^(index_(?:[A-Za-z0-9_]+))$""".r ~ Slash ~ "get_next_response") { index_name =>
      pathEnd {
        post {
          authenticateBasicAsync(realm = auth_realm,
            authenticator = authenticator.authenticator) { user =>
            authorizeAsync(_ =>
              authenticator.hasPermissions(user, index_name, Permissions.read)) {
              entity(as[ResponseRequestIn]) {
                response_request =>
                  val responseService = ResponseService
                  val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
                  onCompleteWithBreaker(breaker)(responseService.getNextResponse(index_name, response_request)) {
                    case Failure(e) =>
                      log.error("index(" + index_name + ") DecisionTableResource: Unable to complete the request: " + e.getMessage)
                      completeResponse(StatusCodes.BadRequest,
                        Option {
                          ResponseRequestOutOperationResult(
                            ReturnMessageData(code = 109, message = e.getMessage),
                            Option {
                              List.empty[ResponseRequestOut]
                            })
                        }
                      )
                    case Success(response_value) =>
                      response_value match {
                        case Some(t) =>
                          if (t.status.code == 200) {
                            completeResponse(StatusCodes.OK, StatusCodes.Gone, t.response_request_out)
                          } else {
                            completeResponse(StatusCodes.NoContent) // no response found
                          }
                        case None =>
                          log.error("index(" + index_name + ") DecisionTableResource: Unable to complete the request")
                          completeResponse(StatusCodes.BadRequest,
                            Option {
                              ResponseRequestOutOperationResult(
                                ReturnMessageData(code = 110, message = "unable to complete the response"),
                                Option {
                                  List.empty[ResponseRequestOut]
                                })
                            }
                          )
                      }
                  }
              }
            }
          }
        }
      }
    }
}

