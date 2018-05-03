package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 10/03/17.
  */

object IndexManagementClient extends ElasticClient {
  val sysRefreshDtIndexSuffix: String = config.getString("es.system_refresh_dt_index_suffix")
  val dtIndexSuffix: String = config.getString("es.dt_index_suffix")
  val kbIndexSuffix: String = config.getString("es.kb_index_suffix")
  val termIndexSuffix: String = config.getString("es.term_index_suffix")
  val enableDeleteIndex: Boolean = config.getBoolean("es.enable_delete_application_index")
}
