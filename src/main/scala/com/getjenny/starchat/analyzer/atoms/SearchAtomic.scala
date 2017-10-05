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
class SearchAtomic(arguments: List[String]) extends AbstractAtomic {
  val state = arguments(0)
  override def toString: String = "search(\"" + state + "\")"
  val isEvaluateNormalized: Boolean = false
  val ref_state: String = state

  override val match_threshold: Double = 0.65

  val decisionTableService = DecisionTableService

  def evaluate(query: String, data: AnalyzersData = AnalyzersData()): Result = {

    val search_res = data.data.getOrElse("dt_queries_search_result", None)
        .asInstanceOf[Option[Map[String, (Float, SearchDTDocument)]]]

    val score = if(search_res.nonEmpty && search_res.get.contains(ref_state)) {
      val doc = search_res.get.get(ref_state)
      doc.get._1 / (search_res.get.map(x => x._2._1).sum + 1)
    } else {
      0.0f
    }

    Result(score=score)
  } // returns elasticsearch score of the highest query in queries

}
