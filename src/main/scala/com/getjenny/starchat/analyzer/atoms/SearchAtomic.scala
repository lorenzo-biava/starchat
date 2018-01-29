package com.getjenny.starchat.analyzer.atoms

/**
  * Created by mal on 20/02/2017.
  */

import com.getjenny.analyzer.atoms.{AbstractAtomic, ExceptionAtomic}
import com.getjenny.analyzer.expressions.{AnalyzersData, Result}
import com.getjenny.starchat.entities._

import scala.concurrent.{Await, ExecutionContext, Future}
import com.getjenny.starchat.services._

import scala.concurrent.duration._
import scala.concurrent._
import ExecutionContext.Implicits.global

/**
  * Query ElasticSearch
  */
class SearchAtomic(arguments: List[String], restricted_args: Map[String, String]) extends AbstractAtomic {
  val state: String = arguments.headOption match {
    case Some(t) => t
    case _ =>
      throw ExceptionAtomic("search requires an argument")
  }

  override def toString: String = "search(\"" + state + "\")"
  val isEvaluateNormalized: Boolean = false

  override val matchThreshold: Double = 0.65

  val decisionTableService: DecisionTableService.type = DecisionTableService

  def evaluate(query: String, data: AnalyzersData = AnalyzersData()): Result = {

    val searchRes = data.data.getOrElse("dt_queries_search_result", None)
        .asInstanceOf[Option[Map[String, (Float, SearchDTDocument)]]]

    val score = searchRes match {
      case Some(t) =>
        t.get(state) match {
          case Some(refState) =>
            val searchScoresSum = t.map{case (_, (docScore, _)) => docScore}.sum + 1
            refState._1 / searchScoresSum
          case _ => 0.0d
        }
    }

    Result(score=score)
  } // returns elasticsearch score of the highest query in queries

}
