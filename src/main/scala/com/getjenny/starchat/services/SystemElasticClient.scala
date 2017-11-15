package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 23/08/17.
  */

object SystemElasticClient extends ElasticClient {
  val system_refresh_dt_type_name: String = config.getString("es.system_refresh_dt_type_name")
  val dt_reload_check_delay : Int = config.getInt("es.dt_reload_check_delay")
  val dt_reload_check_frequency : Int = config.getInt("es.dt_reload_check_frequency")
  val dt_reload_index_name_field_name = "index_name"
  val dt_reload_timestamp_field_name = "state_refresh_ts"
}