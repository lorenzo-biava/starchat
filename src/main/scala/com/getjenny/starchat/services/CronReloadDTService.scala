package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 23/08/17.
  */

import com.getjenny.starchat.entities._

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import akka.event.{Logging, LoggingAdapter}
import com.getjenny.starchat.SCActorSystem
import akka.actor.Actor
import akka.actor.Props
import scala.language.postfixOps

class CronReloadDTService(implicit val executionContext: ExecutionContext) {
  val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)
  val analyzerService: AnalyzerService.type = AnalyzerService
  val systemService: SystemService.type = SystemService

  val Tick = "tick"

  class ReloadAnalyzersTickActor extends Actor {
    def receive: PartialFunction[Any, Unit] = {
      case Tick =>
        analyzerService.analyzersMap.foreach(item => {
          val timestamp_result: Try[Option[Long]] =
            Await.ready(systemService.getDTReloadTimestamp(indexName = item._1), 20.seconds).value.get
          val remoteTs: Long = timestamp_result match {
            case Success(t) =>
              val ts: Long = t.getOrElse(-1)
              if (ts > 0) {
                log.info("dt reload timestamp for index(" + item._1 + "): " + ts)
              }
              ts
            case Failure(e) =>
              log.debug("timestamp for index(" + item._1 + ") not found " + e.getMessage)
              -1: Long
          }

          if (remoteTs > 0 &&
            SystemService.dtReloadTimestamp < remoteTs) {
            val reloadResult: Try[Option[DTAnalyzerLoad]] =
              Await.ready(analyzerService.loadAnalyzer(indexName = item._1), 60.seconds).value.get
            reloadResult match {
              case Success(t) =>
                log.info("Analyzer loaded for index(" + item._1 + "), remote ts: " + remoteTs)
                SystemService.dtReloadTimestamp = remoteTs
              case Failure(e) =>
                log.error("unable to load analyzers for index(" + item._1 + ") from the cron job" + e.getMessage)
            }
          } else {
            analyzerService.analyzersMap.remove(item._1) match {
              case Some(t) =>
                log.info("index_key (" + item._1 + ") last_evaluation_timestamp(" +
                  t.lastEvaluationTimestamp + ") was removed")
              case None =>
                log.error("index_key (" + item._1 + ") not found")
            }
          }
        })
      case _ =>
        log.error("Unknown error reloading analyzers")
    }
  }

  def reloadAnalyzers(): Unit = {
    if (systemService.elasticClient.dtReloadCheckFrequency > 0) {
      val reloadDecisionTableActorRef =
        SCActorSystem.system.actorOf(Props(classOf[ReloadAnalyzersTickActor], this))
      val delay: Int = if(systemService.elasticClient.dtReloadCheckDelay >= 0) {
        systemService.elasticClient.dtReloadCheckDelay
      } else {
        val r = scala.util.Random
        val d = r.nextInt(math.abs(systemService.elasticClient.dtReloadCheckDelay))
        d
      }

      SCActorSystem.system.scheduler.schedule(
        delay seconds,
        systemService.elasticClient.dtReloadCheckFrequency seconds,
        reloadDecisionTableActorRef,
        Tick)
    }
  }
}
