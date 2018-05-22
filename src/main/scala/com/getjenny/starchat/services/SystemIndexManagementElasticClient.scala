package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 14/11/17.
  */

object SystemIndexManagementElasticClient extends ElasticClient {
  val indexName: String = config.getString("es.system_index_name")
  val dtReloadCheckDelay : Int = config.getInt("es.dt_reload_check_delay")
  val dtReloadCheckFrequency : Int = config.getInt("es.dt_reload_check_frequency")
  val dtReloadTimestampFieldName = "state_refresh_ts"
  val authMethod: String = config.getString("starchat.auth_method")
}