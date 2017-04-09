package com.getjenny.starchat.analyzer.atoms

import com.getjenny.analyzer.atoms.AbstractAtomic
import com.getjenny.starchat.analyzer.utils.VectorUtils._
import scala.concurrent.{Await, ExecutionContext, Future}
import com.getjenny.starchat.services._
import ExecutionContext.Implicits.global

/**
  * Created by angelo on 07/04/17.
  */

class W2VEuclideanWordAtomic(word: String) extends AbstractAtomic {
  /**
    * Return the normalized w2vcosine similarity of the nearest word
    */

  override def toString: String = "similar(\"" + word + "\")"

  val termService = new TermService

  val empty_vec = Vector.fill(300){0.0}
  def getTextVector(text: String): Vector[Double] = {
    val text_vectors = termService.textToVectors(text)
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

  val isEvaluateNormalized: Boolean = true
  private val word_vec = getTextVector(word)
  def evaluate(query: String): Double = {
    val text_vectors = termService.textToVectors(query)
    //TODO: reduce the accuracy by dividing the score by the number of missing terms
    val distance: Double = if (text_vectors.nonEmpty && text_vectors.get.terms.nonEmpty) {
      val term_vector = text_vectors.get.terms.get.terms.filter(term => term.vector.nonEmpty)
        .map(term => term.vector.get)
      val distance_list = term_vector.map(vector => {
        if(vector.isEmpty || word_vec.isEmpty) {
          0.0
        } else {
          val eucl_distance = euclideanDist(vector, word_vec)
          if (eucl_distance == 0) 1 else 1.0 / eucl_distance
        }
      })
      distance_list.max
    } else {
      0.0
    }
    distance
  }
  // Similarity is normally the cosine itself. The threshold should be at least
  // angle < pi/2 (cosine > 0), but for synonyms let's put cosine > 0.6, i.e. self.evaluate > 0.8
  override val match_threshold: Double = 0.8
}