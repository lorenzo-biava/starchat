package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 10/03/17.
  */

object IndexManagementClient extends ElasticClient {
  val dt_type_name: String = config.getString("es.dt_type_name")
  val kb_type_name: String = config.getString("es.kb_type_name")
  val term_type_name: String = config.getString("es.term_type_name")
  val enable_delete_index: Boolean = config.getBoolean("es.enable_delete_index")
}
