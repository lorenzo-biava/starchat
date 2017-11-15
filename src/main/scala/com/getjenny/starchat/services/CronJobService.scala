package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 23/08/17.
  */

import com.getjenny.starchat.entities._

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import akka.event.{Logging, LoggingAdapter}
import com.getjenny.starchat.SCActorSystem
import akka.actor.Actor
import akka.actor.Props
import scala.language.postfixOps

class CronJobService (implicit val executionContext: ExecutionContext) {
  val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)
  val analyzerService: AnalyzerService.type = AnalyzerService
  val systemService: SystemService.type = SystemService

  val Tick = "tick"

  class ReloadAnalyzersTickActor extends Actor {
    def receive = {
      case Tick =>
        analyzerService.analyzers_map.foreach(item => {
          val timestamp_result: Try[Option[Long]] =
            Await.ready(systemService.getDTReloadTimestamp(index_name = item._1), 10.seconds).value.get
          val remote_ts: Long = timestamp_result match {
            case Success(t) =>
              val ts: Long = t.getOrElse(-1)
              if (ts > 0) {
                log.info("dt reload timestamp: " + ts)
              }
              ts
            case Failure(e) =>
              log.error("unable to load analyzers from the cron job: " + e.getMessage)
              -1: Long
          }

          if (remote_ts > 0 &&
            SystemService.dt_reload_timestamp < remote_ts) {
            val reload_result: Try[Option[DTAnalyzerLoad]] =
              Await.ready(analyzerService.loadAnalyzer(index_name = item._1), 60.seconds).value.get
            reload_result match {
              case Success(t) =>
                log.info("Analyzer loaded, remote ts: " + remote_ts)
                SystemService.dt_reload_timestamp = remote_ts
              case Failure(e) =>
                log.error("unable to load analyzers from the cron job" + e.getMessage)
            }
          } else {
            log.debug("local dt reload: no update, timestamp(" +
              SystemService.dt_reload_timestamp + ") Remote(" + remote_ts + ")")
          }
        })
      case _ =>
        log.error("Unknown error reloading analyzers")
    }
  }

  def reloadDecisionTable(): Unit = {
    if (systemService.elastic_client.dt_reload_check_frequency > 0) {
      val reloadDecisionTableActorRef = SCActorSystem.system.actorOf(Props(classOf[ReloadAnalyzersTickActor], this))
      val delay: Int = if(systemService.elastic_client.dt_reload_check_delay >= 0) {
        systemService.elastic_client.dt_reload_check_delay
      } else {
        val r = scala.util.Random
        val d = r.nextInt(math.abs(systemService.elastic_client.dt_reload_check_delay))
        d
      }

      SCActorSystem.system.scheduler.schedule(
        delay seconds,
        systemService.elastic_client.dt_reload_check_frequency seconds,
        reloadDecisionTableActorRef,
        Tick)
    } else {
      log.info("reloading DT disabled")
    }
  }
}
