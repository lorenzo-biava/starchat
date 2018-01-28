package com.getjenny.starchat.resources

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 27/06/16.
  */

import java.io.File

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.FileInfo
import akka.pattern.CircuitBreaker
import com.getjenny.starchat.entities._
import com.getjenny.starchat.routing._
import com.getjenny.starchat.services.{AnalyzerService, DecisionTableService, ResponseService}

import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scalaz.Scalaz._

trait DecisionTableResource extends MyResource {

  def decisionTableRoutes: Route =
    pathPrefix("""^(index_(?:[a-z]{1,256})_(?:[A-Za-z0-9_]{1,256}))$""".r ~ Slash ~ "decisiontable") { indexName =>
      pathEnd {
        val decisionTableService = DecisionTableService
        post {
          authenticateBasicAsync(realm = authRealm,
            authenticator = authenticator.authenticator) { user =>
            authorizeAsync(_ =>
              authenticator.hasPermissions(user, indexName, Permissions.write)) {
              parameters("refresh".as[Int] ? 0) { refresh =>
                entity(as[DTDocument]) { document =>
                  val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
                  onCompleteWithBreaker(breaker)(decisionTableService.create(indexName, document, refresh)) {
                    case Success(t) =>
                      completeResponse(StatusCodes.Created, StatusCodes.BadRequest, t)
                    case Failure(e) =>
                      log.error("index(" + indexName + ") route=decisionTableRoutes method=POST: " + e.getMessage)
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
            authenticateBasicAsync(realm = authRealm,
              authenticator = authenticator.authenticator) { user =>
              authorizeAsync(_ =>
                authenticator.hasPermissions(user, indexName, Permissions.read)) {
                parameters("ids".as[String].*, "dump".as[Boolean] ? false) { (ids, dump) =>
                  if (!dump) {
                    val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
                    onCompleteWithBreaker(breaker)(decisionTableService.read(indexName, ids.toList)) {
                      case Success(t) =>
                        completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option {
                          t
                        })
                      case Failure(e) =>
                        log.error("index(" + indexName + ") route=decisionTableRoutes method=GET: " + e.getMessage)
                        completeResponse(StatusCodes.BadRequest,
                          Option {
                            ReturnMessageData(code = 101, message = e.getMessage)
                          })
                    }
                  } else {
                    val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
                    onCompleteWithBreaker(breaker)(decisionTableService.getDTDocuments(indexName)) {
                      case Success(t) =>
                        completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option {
                          t
                        })
                      case Failure(e) =>
                        log.error("index(" + indexName + ") route=decisionTableRoutes method=GET: " + e.getMessage)
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
            authenticateBasicAsync(realm = authRealm,
              authenticator = authenticator.authenticator) { user =>
              authorizeAsync(_ =>
                authenticator.hasPermissions(user, indexName, Permissions.write)) {
                val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
                onCompleteWithBreaker(breaker)(decisionTableService.deleteAll(indexName)) {
                  case Success(t) =>
                    completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option {
                      t
                    })
                  case Failure(e) =>
                    log.error("index(" + indexName + ") route=decisionTableRoutes method=DELETE : " + e.getMessage)
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
            authenticateBasicAsync(realm = authRealm,
              authenticator = authenticator.authenticator) { user =>
              authorizeAsync(_ =>
                authenticator.hasPermissions(user, indexName, Permissions.write)) {
                entity(as[DTDocumentUpdate]) { update =>
                  parameters("refresh".as[Int] ? 0) { refresh =>
                    val decisionTableService = DecisionTableService
                    val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
                    onCompleteWithBreaker(breaker)(decisionTableService.update(indexName, id, update, refresh)) {
                      case Success(t) =>
                        completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option {
                          t
                        })
                      case Failure(e) =>
                        log.error("index(" + indexName + ") route=decisionTableRoutes method=PUT : " + e.getMessage)
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
              authenticateBasicAsync(realm = authRealm,
                authenticator = authenticator.authenticator) { user =>
                authorizeAsync(_ =>
                  authenticator.hasPermissions(user, indexName, Permissions.write)) {
                  parameters("refresh".as[Int] ? 0) { refresh =>
                    val decisionTableService = DecisionTableService
                    val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
                    onCompleteWithBreaker(breaker)(decisionTableService.delete(indexName, id, refresh)) {
                      case Success(t) =>
                        if (t.isDefined) {
                          completeResponse(StatusCodes.OK, t)
                        } else {
                          completeResponse(StatusCodes.BadRequest, t)
                        }
                      case Failure(e) =>
                        log.error("index(" + indexName + ") route=decisionTableRoutes method=DELETE : " + e.getMessage)
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


  def tempDestination(fileInfo: FileInfo): File =
    File.createTempFile("DecisionTableCSV", ".csv")

  def decisionTableUploadCSVRoutes: Route =
    pathPrefix("""^(index_(?:[a-z]{1,256})_(?:[A-Za-z0-9_]{1,256}))$""".r ~ Slash ~ "decisiontable_upload_csv") { indexName =>
      pathEnd {
        authenticateBasicAsync(realm = authRealm,
          authenticator = authenticator.authenticator) { user =>
          authorizeAsync(_ =>
            authenticator.hasPermissions(user, indexName, Permissions.write)) {
            storeUploadedFile("csv", tempDestination) {
              case (metadata, file) =>
                val decisionTableService = DecisionTableService
                val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker(callTimeout = 10.seconds)
                onCompleteWithBreaker(breaker)(decisionTableService.indexCSVFileIntoDecisionTable(indexName, file)) {
                  case Success(t) =>
                    file.delete()
                    completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option {
                      t
                    })
                  case Failure(e) =>
                    log.error("index(" + indexName + ") route=decisionTableUploadCSVRoutes method=POST: " + e.getMessage)
                    if(file.exists()) {
                      file.delete()
                    }
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
    pathPrefix("""^(index_(?:[a-z]{1,256})_(?:[A-Za-z0-9_]{1,256}))$""".r ~ Slash ~ "decisiontable_analyzer") { indexName =>
      pathEnd {
        get {
          authenticateBasicAsync(realm = authRealm,
            authenticator = authenticator.authenticator) { user =>
            authorizeAsync(_ =>
              authenticator.hasPermissions(user, indexName, Permissions.read)) {
              val analyzerService = AnalyzerService
              val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
              onCompleteWithBreaker(breaker)(analyzerService.getDTAnalyzerMap(indexName)) {
                case Success(t) =>
                  completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option {
                    t
                  })
                case Failure(e) =>
                  log.error("index(" + indexName + ") route=decisionTableAnalyzerRoutes method=GET: " + e.getMessage)
                  completeResponse(StatusCodes.BadRequest,
                    Option {
                      ReturnMessageData(code = 106, message = e.getMessage)
                    })
              }
            }
          }
        } ~
          post {
            authenticateBasicAsync(realm = authRealm,
              authenticator = authenticator.authenticator) { user =>
              authorizeAsync(_ =>
                authenticator.hasPermissions(user, indexName, Permissions.write)) {
                val analyzerService = AnalyzerService
                val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
                onCompleteWithBreaker(breaker)(analyzerService.loadAnalyzer(indexName, propagate = true)) {
                  case Success(t) =>
                    completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option {
                      t
                    })
                  case Failure(e) =>
                    log.error("index(" + indexName + ") route=decisionTableAnalyzerRoutes method=POST: " + e.getMessage)
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
    pathPrefix("""^(index_(?:[a-z]{1,256})_(?:[A-Za-z0-9_]{1,256}))$""".r ~ Slash ~ "decisiontable_search") { indexName =>
      pathEnd {
        post {
          authenticateBasicAsync(realm = authRealm,
            authenticator = authenticator.authenticator) { user =>
            authorizeAsync(_ =>
              authenticator.hasPermissions(user, indexName, Permissions.read)) {
              entity(as[DTDocumentSearch]) { docsearch =>
                val decisionTableService = DecisionTableService
                val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
                onCompleteWithBreaker(breaker)(decisionTableService.search(indexName, docsearch)) {
                  case Success(t) =>
                    completeResponse(StatusCodes.OK, StatusCodes.BadRequest, Option {
                      t
                    })
                  case Failure(e) =>
                    log.error("index(" + indexName + ") route=decisionTableSearchRoutes method=POST: " + e.getMessage)
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
    pathPrefix("""^(index_(?:[A-Za-z0-9_]{1,256}))$""".r ~ Slash ~ "get_next_response") { indexName =>
      pathEnd {
        post {
          authenticateBasicAsync(realm = authRealm,
            authenticator = authenticator.authenticator) { user =>
            authorizeAsync(_ =>
              authenticator.hasPermissions(user, indexName, Permissions.read)) {
              entity(as[ResponseRequestIn]) {
                response_request =>
                  val responseService = ResponseService
                  val breaker: CircuitBreaker = StarChatCircuitBreaker.getCircuitBreaker()
                  onCompleteWithBreaker(breaker)(responseService.getNextResponse(indexName, response_request)) {
                    case Failure(e) =>
                      log.error("index(" + indexName + ") DecisionTableResource: Unable to complete the request: " + e.getMessage)
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
                          if (t.status.code === 200) {
                            completeResponse(StatusCodes.OK, StatusCodes.BadRequest, t.response_request_out)
                          } else {
                            completeResponse(StatusCodes.NoContent) // no response found
                          }
                        case None =>
                          log.error("index(" + indexName + ") DecisionTableResource: Unable to complete the request")
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

