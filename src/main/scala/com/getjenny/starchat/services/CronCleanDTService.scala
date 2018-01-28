package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 23/08/17.
  */

import akka.actor.{Actor, Props}
import akka.event.{Logging, LoggingAdapter}
import com.getjenny.starchat.SCActorSystem
import com.getjenny.starchat.services.DecisionTableElasticClient.config

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

class CronCleanDTService(implicit val executionContext: ExecutionContext) {
  val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)
  val analyzerService: AnalyzerService.type = AnalyzerService
  val systemService: SystemService.type = SystemService
  val dtMaxTables: Long = config.getLong("es.dt_max_tables")

  val Tick = "tick"

  class CleanDecisionTablesTickActor extends Actor {
    def receive: PartialFunction[Any, Unit] = {
      case Tick =>
        if(dtMaxTables > 0 && analyzerService.analyzersMap.size < dtMaxTables ) {
          val exeding_items: Long = dtMaxTables - analyzerService.analyzersMap.size
          val items_to_remove =
            analyzerService.analyzersMap.toList.sortBy(_._2.lastEvaluationTimestamp).take(exeding_items.toInt)
          items_to_remove.foreach(item => {
            log.info("removing decisin table: " + item._1)
            analyzerService.analyzersMap.remove(item._1)
          })
        }
      case _ =>
        log.error("Unknown error cleaning decision tables")
    }
  }

  def cleanDecisionTables(): Unit = {
    val reloadDecisionTableActorRef =
      SCActorSystem.system.actorOf(Props(classOf[CleanDecisionTablesTickActor], this))

    SCActorSystem.system.scheduler.schedule(
      0 seconds,
      30 seconds,
      reloadDecisionTableActorRef,
      Tick)
  }
}
