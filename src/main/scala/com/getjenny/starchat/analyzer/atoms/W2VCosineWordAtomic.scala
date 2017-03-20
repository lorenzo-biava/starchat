package com.getjenny.starchat.analyzer.atoms

import com.getjenny.analyzer.atoms.AbstractAtomic
import com.getjenny.starchat.analyzer.utils.Vectors._

/**
  * Created by mal on 20/02/2017.
  */

class W2VCosineWordAtomic(word: String) extends AbstractAtomic {
  /**
    * Return the normalized w2vcosine similarity of the nearest word
    *
    *
    */
  override def toString: String = "similar(\"" + word + "\")"

  val isEvaluateNormalized: Boolean = true
  private val vec = word2Vec(word)
  def evaluate(query: String): Double = (1.0 + query.split("\\s+").map(x => cosineDistance(word2Vec(x), vec)).max) / 2
  // Similarity is normally the cosine itself. The threshold should be at least
  // angle < pi/2 (cosine > 0), but for synonyms let's put cosine > 0.6, i.e. self.evaluate > 0.8
  override val match_threshold: Double = 0.8
}
