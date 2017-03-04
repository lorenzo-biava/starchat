package com.getjenny.starchat.analyzer.atoms

/**
  * Created by mal on 20/02/2017.
  */

import com.getjenny.analyzer.atoms.AbstractAtomic
import com.getjenny.starchat.services.DTElasticClient

/**
  * Query ElasticSearch
  */
class SearchAtomic(queries: List[String]) extends AbstractAtomic {
  override def toString: String = "search(\"" + queries + "\")"
  val isEvaluateNormalized: Boolean = false
  val elastic_client = DTElasticClient
  def evaluate(query: String): Double = {
    3.14
  } // returns elasticsearch score of the highest query in queries
}
  