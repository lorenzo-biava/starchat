package com.getjenny.starchat.analyzer

import com.getjenny.starchat.util.Vectors._

/**
  * Created by mal on 20/02/2017.
  */

/**
  * These are the Atomic components of Analyzer. Atomic can be for instance a Pattern (i.e. a Regex)
  * or a Similarity.
  * Atomics can be composed in Analyzers, like in and(regex > 0, similarity > 0.9)
  */
abstract class Atomic extends Expression {
  val isEvaluateNormalized: Boolean  // does "evaluate" return normalized values?
}

/**
  * Query ElasticSearch
  */
class Search(queries: List[String]) extends Atomic {
  override def toString: String = "search(\"" + queries + "\")"
  val isEvaluateNormalized: Boolean = false
  def evaluate(query: String): Double = 3.14 // returns elasticsearch score of the highest query in queries
}

/** Basically a word separated from the others. E.g.:
  * "pippo" matches "pippo and pluto" but not "pippone and pluto"
  * "pippo.*" matches "pippo and pluto" and "pippone and pluto"
  */
class Keyword(val keyword: String) extends Atomic {
  override def toString: String = "keyword(\"" + keyword + "\")"
  val isEvaluateNormalized: Boolean = true
  private val rx = {"""\b""" + keyword + """\b"""}.r
  def evaluate(query: String): Double = rx.findAllIn(query).toList.length.toDouble / """\S+""".r.findAllIn(query).toList.length
}

class W2VCosineSentence(val sentence: String) extends Atomic  {
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

class W2VCosineWord(word: String) extends Atomic {
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


class RegularExpression(re: String) extends Atomic {
  /**
    * A Regex
    */
  override def toString: String = "regex(\"" + re + "\")"
  val isEvaluateNormalized: Boolean = false
  private val rx = re.r
  def evaluate(query: String): Double = rx.findAllIn(query).toList.length
}
