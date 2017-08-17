package com.getjenny.starchat.analyzer.atoms

/**
  * Created by angelo on 11/04/17.
  */

import com.getjenny.starchat.analyzer.utils.VectorUtils._
import com.getjenny.starchat.analyzer.utils.TextToVectorsTools._
import com.getjenny.starchat.analyzer.utils.TextToVectorsTools
import com.getjenny.analyzer.atoms.AbstractAtomic
import com.getjenny.starchat.entities._

import scala.concurrent.{Await, ExecutionContext, Future}
import com.getjenny.starchat.services._

import scala.concurrent.duration._
import scala.concurrent._
import ExecutionContext.Implicits.global
import com.getjenny.analyzer.expressions.{Data, Result}

class W2VCosineStateAtomic(val state: String) extends AbstractAtomic  {
  /**
    * cosine distance between sentences renormalized at [0, 1]: (cosine + 1)/2
    *
    * state_lost_password_cosine = Cosine("lost password")
    * state_lost_password_cosine.evaluate("I'm desperate, I've lost my password")
    *
    */

  override def toString: String = "similarState(\"" + state + "\")"

  val analyzerService = new AnalyzerService

  val query_sentences = AnalyzerService.analyzer_map.getOrElse(state, null)
  if (query_sentences == null) {
    analyzerService.log.error(toString + " : state is null")
  } else {
    analyzerService.log.info(toString + " : initialized")
  }

  val query_terms = query_sentences.queries
  val query_vectors = query_terms.map(item => {
    val query_vector = TextToVectorsTools.getSumOfTermsVectors(Option{item})
    query_vector
  })

  val isEvaluateNormalized: Boolean = true
  def evaluate(query: String, data: Data = Data()): Result = {
    val distance = query_vectors.map(q_item => {
      val query_vector = TextToVectorsTools.getSumOfVectorsFromText(query)
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