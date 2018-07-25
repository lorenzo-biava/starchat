package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 23/08/17.
  */

import akka.actor.{Actor, Props}
import akka.event.{Logging, LoggingAdapter}
import com.getjenny.starchat.SCActorSystem

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

object CronReloadDTService  {
  implicit def executionContext: ExecutionContext = SCActorSystem.system.dispatcher
  private[this] val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)
  private[this] val analyzerService: AnalyzerService.type = AnalyzerService
  private[this] val dtReloadService: DtReloadService.type = DtReloadService
  private[this] val systemIndexManagementService: SystemIndexManagementService.type = SystemIndexManagementService
  private[this] var updateTimestamp: Long = 0

  private[this] val tickMessage: String = "tick"

  class ReloadAnalyzersTickActor extends Actor {
    def receive: PartialFunction[Any, Unit] = {
      case `tickMessage` =>
        val maxItemsIndexesToUpdate: Long = if (analyzerService.dtMaxTables > analyzerService.analyzersMap.size) {
          analyzerService.dtMaxTables - analyzerService.analyzersMap.size
        } else 0L

        dtReloadService.allDTReloadTimestamp(Some(updateTimestamp), Some(maxItemsIndexesToUpdate)) match {
          case Some(indices) =>
            indices.foreach { dtReloadEntry =>
              val indexAnalyzers: Option[ActiveAnalyzers] =
                analyzerService.analyzersMap.get(dtReloadEntry.indexName)
              val localReloadTimestamp = indexAnalyzers match {
                case Some(ts) => ts.lastReloadingTimestamp
                case _ => dtReloadService.DT_RELOAD_TIMESTAMP_DEFAULT
              }

              if (dtReloadEntry.timestamp > 0 && localReloadTimestamp < dtReloadEntry.timestamp) {
                log.info("dt reloading, timestamp for index(" + dtReloadEntry.indexName + "): "
                  + dtReloadEntry.timestamp)

                analyzerService.loadAnalyzers(indexName = dtReloadEntry.indexName).onComplete {
                  case Success(relRes) =>
                    relRes match {
                      case Some(res) =>
                        updateTimestamp = math.max(updateTimestamp, localReloadTimestamp)
                        log.info("Analyzer loaded for index(" + dtReloadEntry + "), res(" + res +
                          ") remote ts: " + dtReloadEntry )
                        analyzerService.analyzersMap(dtReloadEntry.indexName)
                          .lastReloadingTimestamp = dtReloadEntry.timestamp
                      case _ => log.error("ReloadAnalyzersTickActor: getting an empty response reloading analyzers")
                    }
                  case Failure(e) =>
                    log.error("unable to load analyzers for index(" + dtReloadEntry +
                      ") from the cron job" + e.getMessage)
                }
              }
            }
          case _ =>
            log.debug("No entries on dt reload index")
        }
    }
  }

  def reloadAnalyzers(): Unit = {
    if (systemIndexManagementService.elasticClient.dtReloadCheckFrequency > 0) {
      val reloadDecisionTableActorRef =
        SCActorSystem.system.actorOf(Props(new ReloadAnalyzersTickActor))
      val delay: Int = if(systemIndexManagementService.elasticClient.dtReloadCheckDelay >= 0) {
        systemIndexManagementService.elasticClient.dtReloadCheckDelay
      } else {
        val r = scala.util.Random
        val d = r.nextInt(math.abs(systemIndexManagementService.elasticClient.dtReloadCheckDelay))
        d
      }

      SCActorSystem.system.scheduler.schedule(
        delay seconds,
        systemIndexManagementService.elasticClient.dtReloadCheckFrequency seconds,
        reloadDecisionTableActorRef,
        tickMessage)
    }
  }

  def reloadAnalyzersOnce(): Unit = {
    val updateEventsActorRef = SCActorSystem.system.actorOf(Props(new ReloadAnalyzersTickActor))
    SCActorSystem.system.scheduler.scheduleOnce(0 seconds, updateEventsActorRef, tickMessage)
  }

}
