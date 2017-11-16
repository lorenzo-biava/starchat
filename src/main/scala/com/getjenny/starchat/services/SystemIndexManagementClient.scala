package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 14/11/17.
  */

object SystemIndexManagementClient extends ElasticClient {
  val index_name: String = config.getString("es.system_index_name")
  val system_refresh_dt_index_suffix: String = config.getString("es.system_refresh_dt_index_suffix")
  val enable_delete_index: Boolean = config.getBoolean("es.enable_delete_system_index")
}