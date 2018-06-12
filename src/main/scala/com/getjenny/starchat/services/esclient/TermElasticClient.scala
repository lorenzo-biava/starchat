package com.getjenny.starchat.services.esclient

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 12/03/17.
  */

object TermElasticClient extends ElasticClient {
  val indexSuffix: String = termIndexSuffix
}
