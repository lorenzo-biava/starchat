package com.getjenny.starchat.analyzer.atoms

/**
  * Created by angelo on 11/04/17.
  */

import com.getjenny.starchat.analyzer.utils.VectorUtils._
import com.getjenny.starchat.analyzer.utils.TextToVectorsTools._
import com.getjenny.starchat.analyzer.utils.TextToVectorsTools

import com.getjenny.analyzer.atoms.AbstractAtomic
import com.getjenny.starchat.analyzer.utils.EmDistance

import scala.concurrent.{Await, ExecutionContext, Future}
import com.getjenny.starchat.services._

import ExecutionContext.Implicits.global

class W2VEarthMoversEuclideanDistanceStateAtomic(val state: String) extends AbstractAtomic  {
  /**
    * cosine distance between sentences renormalized at [0, 1]: (cosine + 1)/2
    *
    * state_lost_password_cosine = Cosine("lost password")
    * state_lost_password_cosine.evaluate("I'm desperate, I've lost my password")
    *
    */

  val termService = new TermService

  implicit class Crosstable[X](xs: Traversable[X]) {
    def cross[Y](ys: Traversable[Y]) = for { x <- xs; y <- ys } yield (x, y)
  }

  override def toString: String = "similarEucEmdState(\"" + state + "\")"

  val analyzerService = new AnalyzerService

  val queries_sentences = AnalyzerService.analyzer_map.getOrElse(state, null)
  if (queries_sentences == null) {
    analyzerService.log.error(toString + " : state is null")
  } else {
    analyzerService.log.info(toString + " : initialized")
  }

  val queries_terms = queries_sentences.queries
  val queries_vectors = queries_terms.map(item => {
    val query_vector = TextToVectorsTools.getSumOfTermsVectors(Option{item})
    query_vector
  })

  val isEvaluateNormalized: Boolean = true
  def evaluate(query: String): Double = {
    val query_vector = TextToVectorsTools.getSumOfVectorsFromText(query)
    val emd_dist = queries_vectors.map(item => {
      val distance = (1.0 - euclideanDist(query_vector._1, item._1)) * (query_vector._2 * item._2)
//      val score = (1.0 - emd_dist._1) * (emd_dist._2 * emd_dist._3)
      distance
    }).max
    emd_dist
  }

  // Similarity is normally the cosine itself. The threshold should be at least

  // angle < pi/2 (cosine > 0), but for synonyms let's put cosine > 0.6, i.e. self.evaluate > 0.8
  override val match_threshold: Double = 0.8
}