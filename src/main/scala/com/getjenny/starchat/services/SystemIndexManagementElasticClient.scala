package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 14/11/17.
  */

object SystemIndexManagementElasticClient extends ElasticClient {
  val indexName: String = config.getString("es.system_index_name")

  val dtReloadCheckFrequency : Int = config.getInt("es.dt_reload_check_frequency")
  val dtReloadTimestampFieldName = "state_refresh_ts"
  val systemRefreshDtIndexSuffix: String = config.getString("es.system_refresh_dt_index_suffix")

  val userIndexSuffix: String = config.getString("es.user_index_suffix")
  val enableDeleteIndex: Boolean = config.getBoolean("es.enable_delete_system_index")
  val authMethod: String = config.getString("starchat.auth_method")
}