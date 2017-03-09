package com.getjenny.starchat.analyzer.atoms

/**
  * Created by mal on 20/02/2017.
  */

import com.getjenny.analyzer.atoms.AbstractAtomic
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
  val dtElasticService = new DecisionTableService
  val ref_state: String = state

  def evaluate(query: String): Double = {
    val min_score = Option{dtElasticService.elastic_client.query_min_threshold}
    val boost_exact_match_factor = Option{dtElasticService.elastic_client.boost_exact_match_factor}

    val dtDocumentSearch : DTDocumentSearch =
      DTDocumentSearch(from = Option{0}, size = Option{10}, min_score = min_score,
        boost_exact_match_factor = boost_exact_match_factor, state = Option{ref_state}, queries = Option{query})

    val state: Future[Option[SearchDTDocumentsResults]] = dtElasticService.search(dtDocumentSearch)
    //search the state with the closest query value, then return that state
    val res : Option[SearchDTDocumentsResults] = Await.result(state, 30.seconds)

    val res_count = if (res.isEmpty) 0 else res.get.total

    val score : Float = res_count match {
      case 0 => 0.0f
      case _ =>
        val doc : DTDocument = res.get.hits.head.document
        val state : String = doc.state
        val max_score : Float = res.get.max_score
        val sum_of_scores : Float = res.get.hits.map(x => x.score).sum
        max_score / sum_of_scores
    }
    score
  } // returns elasticsearch score of the highest query in queries

}
  