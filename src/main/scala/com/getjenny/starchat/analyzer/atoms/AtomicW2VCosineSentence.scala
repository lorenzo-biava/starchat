package com.getjenny.starchat.analyzer.atoms

import com.getjenny.starchat.util.Vectors._

/**
  * Created by mal on 20/02/2017.
  */

class AtomicW2VCosineSentence(val sentence: String) extends AbstractAtomic  {
  /**
    * cosine distance between sentences renormalized at [0, 1]: (cosine + 1)/2
    *
    * state_lost_password_cosine = Cosine("lost password")
    * state_lost_password_cosine.evaluate("I'm desperate, I've lost my password")
    *
    */
  override def toString: String = "similar(\"" + sentence + "\")"
  val isEvaluateNormalized: Boolean = true
  def evaluate(query: String): Double = (cosineDistance(sentence2Vec(sentence), sentence2Vec(query)) + 1)/2

  // Similarity is normally the cosine itself. The threshold should be at least
  // angle < pi/2 (cosine > 0), but for synonyms let's put cosine > 0.6, i.e. self.evaluate > 0.8
  override val match_threshold: Double = 0.8
}
