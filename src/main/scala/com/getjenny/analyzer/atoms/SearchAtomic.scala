package com.getjenny.analyzer.atoms

/**
  * Created by mal on 20/02/2017.
  */

/**
  * Query ElasticSearch
  */
class SearchAtomic(queries: List[String]) extends AbstractAtomic {
  override def toString: String = "search(\"" + queries + "\")"
  val isEvaluateNormalized: Boolean = false
  def evaluate(query: String): Double = 3.14 // returns elasticsearch score of the highest query in queries
}
  