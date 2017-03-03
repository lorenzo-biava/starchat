package com.getjenny.starchat.analyzer.atoms

import com.getjenny.starchat.util.Vectors._

/**
  * Created by mal on 20/02/2017.
  */

/**
  * Query ElasticSearch
  */
class AtomicSearch(queries: List[String]) extends AbstractAtomic {
  override def toString: String = "search(\"" + queries + "\")"
  val isEvaluateNormalized: Boolean = false
  def evaluate(query: String): Double = 3.14 // returns elasticsearch score of the highest query in queries
}
