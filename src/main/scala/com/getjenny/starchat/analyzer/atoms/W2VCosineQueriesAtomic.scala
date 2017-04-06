package com.getjenny.starchat.analyzer.atoms

import com.getjenny.starchat.analyzer.utils.VectorUtils._
import com.getjenny.analyzer.atoms.AbstractAtomic
import com.getjenny.starchat.entities._

import scala.concurrent.{Await, ExecutionContext, Future}
import com.getjenny.starchat.services._

import scala.concurrent.duration._
import scala.concurrent._
import ExecutionContext.Implicits.global

/**
  * Created by angelo on 05/04/17.
  */

class W2VCosineQueriesAtomic(val state: String) extends AbstractAtomic  {
  /**
    * cosine distance between sentences renormalized at [0, 1]: (cosine + 1)/2
    *
    * state_lost_password_cosine = Cosine("lost password")
    * state_lost_password_cosine.evaluate("I'm desperate, I've lost my password")
    *
    */

  val dtTermService = new TermService
  val dtElasticService = new DecisionTableService

  override def toString: String = "similar_state(\"" + state + "\")"

  val empty_vec = Vector.fill(300){0.0}
  def getTextVector(text: String): Vector[Double] = {
    val text_vectors = dtTermService.textToVectors(text)
    val vector = text_vectors match {
      case Some(t) => {
        val vectors = t.terms.get.terms.map(e => e.vector.get).toVector
        val sentence_vector =
          if (vectors.length > 0) sumArrayOfArrays(vectors) else empty_vec
        sentence_vector
      }
      case _ => empty_vec //default dimension
    }
    vector
  }

  val query_sentences = dtElasticService.analyzer_map.getOrElse(state, null)
  if (query_sentences == null) {
    dtElasticService.log.error("state is null")
  } else {
    dtElasticService.log.info("initialization of: " + toString)
  }

  val vector_terms = query_sentences.queries.map(q => q.terms)
    .filter(item => item.nonEmpty)

  val isEvaluateNormalized: Boolean = true
  def evaluate(query: String): Double = {
    val query_vector = getTextVector(query)
    val distances = vector_terms.map(item => {
      val vector_terms = item.get.terms.toVector
        .filter(v => v.vector.nonEmpty).map(x => x.vector.get)
      val sentence_vector = sumArrayOfArrays(vector_terms)
      val distance = 1 - cosineDist(sentence_vector, query_vector)
      distance
    })

    val max_value = distances.max
    max_value
  }

  // Similarity is normally the cosine itself. The threshold should be at least
  // angle < pi/2 (cosine > 0), but for synonyms let's put cosine > 0.6, i.e. self.evaluate > 0.8
  override val match_threshold: Double = 0.8
}
