package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 23/08/17.
  */

object SystemElasticClient extends ElasticClient {
  val systemRefreshDtIndexSuffix: String = config.getString("es.system_refresh_dt_index_suffix")
  val dtReloadCheckDelay : Int = config.getInt("es.dt_reload_check_delay")
  val dtReloadCheckFrequency : Int = config.getInt("es.dt_reload_check_frequency")
  val dtReloadIndexNameFieldName = "index_name"
  val dtReloadTimestampFieldName = "state_refresh_ts"
}