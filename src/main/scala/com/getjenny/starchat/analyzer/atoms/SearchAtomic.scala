package com.getjenny.starchat.analyzer.atoms

/**
  * Created by mal on 20/02/2017.
  */

import com.getjenny.analyzer.atoms.AbstractAtomic
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
  val state = arguments(0)
  override def toString: String = "search(\"" + state + "\")"
  val isEvaluateNormalized: Boolean = false
  val refState: String = state

  override val matchThreshold: Double = 0.65

  val decisionTableService: DecisionTableService.type = DecisionTableService

  def evaluate(query: String, data: AnalyzersData = AnalyzersData()): Result = {

    val searchRes = data.data.getOrElse("dt_queries_search_result", None)
        .asInstanceOf[Option[Map[String, (Float, SearchDTDocument)]]]

    val score = if(searchRes.nonEmpty && searchRes.get.contains(refState)) {
      val doc = searchRes.get.get(refState)
      doc.get._1 / (searchRes.get.map(x => x._2._1).sum + 1)
    } else {
      0.0f
    }

    Result(score=score)
  } // returns elasticsearch score of the highest query in queries

}
