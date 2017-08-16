package com.getjenny.starchat.analyzer.atoms

/**
  * Created by mal on 20/02/2017.
  */

import com.getjenny.analyzer.atoms.AbstractAtomic
import com.getjenny.analyzer.expressions.{Data, Result}
import com.getjenny.starchat.entities._

import scala.concurrent.{Await, ExecutionContext, Future}
import com.getjenny.starchat.services._

import scala.concurrent.duration._
import scala.concurrent._
import ExecutionContext.Implicits.global

/**
  * Query ElasticSearch
  */
class SearchAtomic(state: String) extends AbstractAtomic {
  override def toString: String = "search(\"" + state + "\")"
  val isEvaluateNormalized: Boolean = false
  val ref_state: String = state

  override val match_threshold: Double = 0.65

  val decisionTableService = new DecisionTableService

  def evaluate(query: String, data: Data = Data()): Result = {
    val min_score = Option{decisionTableService.elastic_client.query_min_threshold}
    val boost_exact_match_factor = Option{decisionTableService.elastic_client.boost_exact_match_factor}

    val dtDocumentSearch : DTDocumentSearch =
      DTDocumentSearch(from = Option{0}, size = Option{10}, min_score = min_score,
        execution_order = None: Option[Int],
        boost_exact_match_factor = boost_exact_match_factor, state = Option{ref_state}, queries = Option{query})

    val state: Future[Option[SearchDTDocumentsResults]] = decisionTableService.search(dtDocumentSearch)
    //search the state with the closest query value, then return that state
    val res : Option[SearchDTDocumentsResults] = Await.result(state, 60.seconds)

    val res_count = if (res.isEmpty) 0 else res.get.total

    val score : Double = res_count match {
      case 0 => 0.0f
      case _ =>
        val doc : DTDocument = res.get.hits.head.document
        val state : String = doc.state
        val max_score : Double = res.get.max_score
        val sum_of_scores : Double = res.get.hits.map(x => x.score).sum
        val norm_score = max_score / (sum_of_scores + 1)
        norm_score
    }
    Result(score=score)
  } // returns elasticsearch score of the highest query in queries

}
