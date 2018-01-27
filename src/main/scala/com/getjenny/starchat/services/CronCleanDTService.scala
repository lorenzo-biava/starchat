package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 23/08/17.
  */

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import akka.event.{Logging, LoggingAdapter}
import com.getjenny.starchat.SCActorSystem
import akka.actor.Actor
import akka.actor.Props
import scala.language.postfixOps
import com.getjenny.starchat.services.DecisionTableElasticClient.config

class CronCleanDTService(implicit val executionContext: ExecutionContext) {
  val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)
  val analyzerService: AnalyzerService.type = AnalyzerService
  val systemService: SystemService.type = SystemService
  val dt_max_tables: Long = config.getLong("es.dt_max_tables")

  val Tick = "tick"

  class CleanDecisionTablesTickActor extends Actor {
    def receive: PartialFunction[Any, Unit] = {
      case Tick =>
        if(dt_max_tables > 0 && analyzerService.analyzersMap.size < dt_max_tables ) {
          val exeding_items: Long = dt_max_tables - analyzerService.analyzersMap.size
          val items_to_remove =
            analyzerService.analyzersMap.toList.sortBy(_._2.last_evaluation_timestamp).take(exeding_items.toInt)
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
