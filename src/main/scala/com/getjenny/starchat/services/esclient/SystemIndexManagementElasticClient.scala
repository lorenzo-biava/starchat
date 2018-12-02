package com.getjenny.starchat.services.esclient

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 14/11/17.
  */

object SystemIndexManagementElasticClient extends ElasticClient {
  override val indexName: String = config.getString("es.system_index_name")
  override val indexSuffix: String = ""
  override val indexMapping: String = ""
  val dtReloadCheckFrequency : Int = config.getInt("es.dt_reload_check_frequency")
  val dtReloadTimestampFieldName = "state_refresh_ts"
  val authMethod: String = config.getString("starchat.auth_method")
}
