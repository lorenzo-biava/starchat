package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 14/11/17.
  */

object SystemIndexManagementElasticClient extends ElasticClient {
  val index_name: String = config.getString("es.system_index_name")
  val user_index_suffix: String = config.getString("es.user_index_suffix")
  val system_refresh_dt_index_suffix: String = config.getString("es.system_refresh_dt_index_suffix")
  val enable_delete_index: Boolean = config.getBoolean("es.enable_delete_system_index")
  val auth_method: String = config.getString("starchat.auth_method")
}