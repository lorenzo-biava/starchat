package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 12/03/17.
  */

object TermClient extends ElasticClient {
  val termIndexSuffix: String = config.getString("es.term_index_suffix")
}
