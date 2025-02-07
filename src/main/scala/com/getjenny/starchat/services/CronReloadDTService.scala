package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 23/08/17.
  */

import akka.actor.{Actor, Props}
import com.getjenny.starchat.SCActorSystem

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

object CronReloadDTService extends CronService {

  class ReloadAnalyzersTickActor extends Actor {
    protected[this] var updateTimestamp: Long = -1

    def receive: PartialFunction[Any, Unit] = {
      case `tickMessage` =>
        val startUpdateTimestamp: Long = System.currentTimeMillis
        val maxItemsIndexesToUpdate: Long = math.max(analyzerService.dtMaxTables, analyzerService.analyzersMap.size)

        log.debug("Start DT reloading session: " + startUpdateTimestamp + " items(" + maxItemsIndexesToUpdate + ")")

        dtReloadService.allDTReloadTimestamp(Some(updateTimestamp), Some(maxItemsIndexesToUpdate))
          .foreach { dtReloadEntry =>
            val indexAnalyzers: Option[ActiveAnalyzers] =
              analyzerService.analyzersMap.get(dtReloadEntry.indexName)
            val localReloadIndexTimestamp = indexAnalyzers match {
              case Some(ts) => ts.lastReloadingTimestamp
              case _ => dtReloadService.DT_RELOAD_TIMESTAMP_DEFAULT
            }

            if (dtReloadEntry.timestamp > 0 && localReloadIndexTimestamp < dtReloadEntry.timestamp) {
              log.info("dt reloading for index(" + dtReloadEntry.indexName +
                ") timestamp (" + startUpdateTimestamp + ") : " + dtReloadEntry.timestamp)

              Try(Await.result(analyzerService.loadAnalyzers(indexName = dtReloadEntry.indexName), 120.second)) match {
                case Success(relRes) =>
                  updateTimestamp = math.max(updateTimestamp, localReloadIndexTimestamp)
                  log.info("Analyzer loaded for index(" + dtReloadEntry + "), timestamp (" +
                    startUpdateTimestamp + ") res(" + relRes + ") remote ts: " + dtReloadEntry )
                  analyzerService.analyzersMap(dtReloadEntry.indexName)
                    .lastReloadingTimestamp = dtReloadEntry.timestamp
                case Failure(e) =>
                  log.error("unable to load analyzers for index(" + dtReloadEntry +
                    "), timestamp(" + startUpdateTimestamp + "), cron job" + e.getMessage)
              }

            }
          }
    }
  }

  def scheduleAction: Unit = {
    if (systemIndexManagementService.elasticClient.dtReloadCheckFrequency > 0) {
      val reloadDecisionTableActorRef =
        SCActorSystem.system.actorOf(Props(new ReloadAnalyzersTickActor))
      SCActorSystem.system.scheduler.schedule(
        0 seconds,
        systemIndexManagementService.elasticClient.dtReloadCheckFrequency seconds,
        reloadDecisionTableActorRef,
        tickMessage)
    }
  }
}
