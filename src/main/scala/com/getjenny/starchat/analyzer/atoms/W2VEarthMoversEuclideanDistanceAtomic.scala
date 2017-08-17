package com.getjenny.starchat.analyzer.atoms

import com.getjenny.starchat.analyzer.utils.EmDistance
import com.getjenny.analyzer.atoms.AbstractAtomic

import scala.concurrent.{Await, ExecutionContext, Future}
import com.getjenny.starchat.services._

import ExecutionContext.Implicits.global
import com.getjenny.analyzer.expressions.{Data, Result}

/**
  * Created by angelo on 04/04/17.
  */

class W2VEarthMoversEuclideanDistanceAtomic(val sentence: String) extends AbstractAtomic  {
  /**
    * cosine distance between sentences renormalized at [0, 1]: (cosine + 1)/2
    *
    * state_lost_password_cosine = Cosine("lost password")
    * state_lost_password_cosine.evaluate("I'm desperate, I've lost my password")
    *
    */

  val termService = new TermService

  override def toString: String = "similarEucEmd(\"" + sentence + "\")"
  val isEvaluateNormalized: Boolean = true
  def evaluate(query: String, data: Data = Data()): Result = {
    val emd_dist = EmDistance.distanceEuclidean(query, sentence)
    Result(score=emd_dist)
  }

  // Similarity is normally the cosine itself. The threshold should be at least

  // angle < pi/2 (cosine > 0), but for synonyms let's put cosine > 0.6, i.e. self.evaluate > 0.8
  override val match_threshold: Double = 0.8
}