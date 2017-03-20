package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 10/03/17.
  */

object TermTableElasticClient extends ElasticClient {
  val type_name: String = config.getString("es.term_type_name")
}
