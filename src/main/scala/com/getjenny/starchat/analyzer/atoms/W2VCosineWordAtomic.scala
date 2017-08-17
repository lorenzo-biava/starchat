package com.getjenny.starchat.analyzer.atoms
import com.getjenny.analyzer.atoms.AbstractAtomic
import com.getjenny.starchat.analyzer.utils.TextToVectorsTools
import com.getjenny.starchat.analyzer.utils.VectorUtils._
import com.getjenny.starchat.analyzer.utils.TextToVectorsTools._

import scala.concurrent.{Await, ExecutionContext, Future}
import com.getjenny.starchat.services._
import com.getjenny.analyzer.expressions.{Data, Result}

import ExecutionContext.Implicits.global


/**
  * Created by mal on 20/02/2017.
  */

class W2VCosineWordAtomic(word: String) extends AbstractAtomic {
  /**
    * Return the normalized w2vcosine similarity of the nearest word
    */

  override def toString: String = "similar(\"" + word + "\")"

  val termService = new TermService

  val isEvaluateNormalized: Boolean = true
  private val word_vec = TextToVectorsTools.getSumOfVectorsFromText(word)
  def evaluate(query: String, data: Data = Data()): Result = {
    val text_vectors = termService.textToVectors(query)
    val distance: Double = if (text_vectors.nonEmpty && text_vectors.get.terms.nonEmpty) {
      val term_vector = text_vectors.get.terms.get.terms.filter(term => term.vector.nonEmpty)
        .map(term => term.vector.get)
      val distance_list = term_vector.map(vector => {
        if(vector.isEmpty || word_vec._2 == 0.0) {
          0.0
        } else {
          1 - cosineDist(vector, word_vec._1)
        }
      })
      val dist = if (distance_list.nonEmpty) distance_list.max else 0.0
      dist
    } else {
      0.0
    }
    Result(score=distance)
  }
  // Similarity is normally the cosine itself. The threshold should be at least
  // angle < pi/2 (cosine > 0), but for synonyms let's put cosine > 0.6, i.e. self.evaluate > 0.8
  override val match_threshold: Double = 0.8
}
