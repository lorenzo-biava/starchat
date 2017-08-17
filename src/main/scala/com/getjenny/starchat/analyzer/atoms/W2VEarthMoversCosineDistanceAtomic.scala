package com.getjenny.starchat.analyzer.atoms

import com.getjenny.starchat.analyzer.utils.VectorUtils._
import com.getjenny.starchat.analyzer.utils.EmDistance._
import com.getjenny.analyzer.atoms.AbstractAtomic
import com.getjenny.starchat.analyzer.utils.EmDistance

import scala.concurrent.{Await, ExecutionContext, Future}
import com.getjenny.starchat.services._
import com.getjenny.analyzer.expressions.{Data, Result}

import ExecutionContext.Implicits.global

/**
  * Created by angelo on 04/04/17.
  */

class W2VEarthMoversCosineDistanceAtomic(val sentence: String) extends AbstractAtomic  {
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

  override def toString: String = "similarCosEmd(\"" + sentence + "\")"
  val isEvaluateNormalized: Boolean = true
  def evaluate(query: String, data: Data = Data()): Result = {
    val emd_dist = EmDistance.distanceCosine(query, sentence)
    Result(score=emd_dist)
  }

  // Similarity is normally the cosine itself. The threshold should be at least

  // angle < pi/2 (cosine > 0), but for synonyms let's put cosine > 0.6, i.e. self.evaluate > 0.8
  override val match_threshold: Double = 0.8
}