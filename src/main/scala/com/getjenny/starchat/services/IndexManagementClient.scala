package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 10/03/17.
  */

object IndexManagementClient extends ElasticClient {
  val sys_refresh_dt_index_suffix: String = config.getString("es.system_refresh_dt_index_suffix")
  val dt_index_suffix: String = config.getString("es.dt_index_suffix")
  val kb_index_suffix: String = config.getString("es.kb_index_suffix")
  val term_index_suffix: String = config.getString("es.term_index_suffix")
  val enable_delete_index: Boolean = config.getBoolean("es.enable_delete_application_index")
}
