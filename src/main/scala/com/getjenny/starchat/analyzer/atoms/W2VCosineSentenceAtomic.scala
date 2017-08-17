package com.getjenny.starchat.analyzer.atoms

import com.getjenny.starchat.analyzer.utils.VectorUtils._
import com.getjenny.starchat.analyzer.utils.TextToVectorsTools._
import com.getjenny.analyzer.atoms.AbstractAtomic
import com.getjenny.starchat.analyzer.utils.TextToVectorsTools
import com.getjenny.starchat.entities._

import scala.concurrent.{Await, ExecutionContext, Future}
import com.getjenny.starchat.services._

import scala.concurrent.duration._
import scala.concurrent._
import ExecutionContext.Implicits.global
import com.getjenny.analyzer.expressions.Result
import com.getjenny.analyzer.expressions.Data

/**
  * Created by mal on 20/02/2017.
  */

class W2VCosineSentenceAtomic(val sentence: String) extends AbstractAtomic  {
  /**
    * cosine distance between sentences renormalized at [0, 1]: (cosine + 1)/2
    *
    * state_lost_password_cosine = Cosine("lost password")
    * state_lost_password_cosine.evaluate("I'm desperate, I've lost my password")
    *
    */

  val termService = new TermService

  val sentence_vector = TextToVectorsTools.getSumOfVectorsFromText(sentence)

  override def toString: String = "similar(\"" + sentence + "\")"
  val isEvaluateNormalized: Boolean = true

  def evaluate(query: String, data: Data = Data()): Result = {
    val query_vector = TextToVectorsTools.getSumOfVectorsFromText(query)

    /** cosineDist returns 0.0 for the closest vector, we want 1.0 when the similarity is the highest
      *   so we use 1.0 - ...
      */
    val distance = (1.0 - cosineDist(sentence_vector._1, query_vector._1)) *
      (sentence_vector._2 * query_vector._2) /** <-- these terms are 1.0 when all vector for all terms of the sentence were found */
    Result(score=distance)
  }

  // Similarity is normally the cosine itself. The threshold should be at least
  // angle < pi/2 (cosine > 0), but for synonyms let's put cosine > 0.6, i.e. self.evaluate > 0.8
  override val match_threshold: Double = 0.8
}
