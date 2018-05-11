package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 12/03/17.
  */

object TermClient extends ElasticClient {
  val commonIndexArbitraryPattern: String = config.getString("es.common_index_arbitrary_pattern")
  val termIndexSuffix: String = config.getString("es.term_index_suffix")
}
