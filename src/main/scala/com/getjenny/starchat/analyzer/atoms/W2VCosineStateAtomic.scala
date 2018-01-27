package com.getjenny.starchat.analyzer.atoms

/**
  * Created by angelo on 11/04/17.
  */

import com.getjenny.analyzer.util.VectorUtils._
import com.getjenny.starchat.analyzer.utils.TextToVectorsTools._
import com.getjenny.starchat.analyzer.utils.TextToVectorsTools
import com.getjenny.analyzer.atoms.AbstractAtomic
import com.getjenny.starchat.entities._

import scala.concurrent.{Await, ExecutionContext, Future}
import com.getjenny.starchat.services._

import scala.concurrent.duration._
import scala.concurrent._
import ExecutionContext.Implicits.global
import com.getjenny.analyzer.expressions.{AnalyzersData, Result}

class W2VCosineStateAtomic(val arguments: List[String], restricted_args: Map[String, String]) extends AbstractAtomic  {
  /**
    * cosine distance between sentences renormalized at [0, 1]: (cosine + 1)/2
    *
    * state_lost_password_cosine = Cosine("lost password")
    * state_lost_password_cosine.evaluate("I'm desperate, I've lost my password")
    *
    */

  val state: String = arguments.head
  override def toString: String = "similarState(\"" + state + "\")"

  val analyzerService: AnalyzerService.type = AnalyzerService

  val index_name = restricted_args("index_name")
  val query_sentences: Option[DecisionTableRuntimeItem] =
    AnalyzerService.analyzers_map(index_name).analyzer_map.get(state)
  if (query_sentences.isEmpty) {
    analyzerService.log.error(toString + " : state does not exists")
  } else {
    analyzerService.log.info(toString + " : initialized")
  }

  val query_terms: List[TextTerms] = query_sentences match {
    case Some(t) => t.queries
    case _ => List.empty[TextTerms]
  }

  val query_vectors: List[(Vector[Double], Double)] = query_terms.map(item => {
    val query_vector = TextToVectorsTools.getSumOfTermsVectors(Option{item})
    query_vector
  })

  val isEvaluateNormalized: Boolean = true
  def evaluate(query: String, data: AnalyzersData = AnalyzersData()): Result = {
    val distance = query_vectors.map(q_item => {
      val query_vector = TextToVectorsTools.getSumOfVectorsFromText(index_name, query)
      val dist = (1.0 - cosineDist(q_item._1, query_vector._1)) *
        (q_item._2 * query_vector._2)
      dist
    })
    val dist = if (distance.nonEmpty) distance.max else 0.0
    Result(score=dist)
  }

  // Similarity is normally the cosine itself. The threshold should be at least
  // angle < pi/2 (cosine > 0), but for synonyms let's put cosine > 0.6, i.e. self.evaluate > 0.8
  override val match_threshold: Double = 0.8
}