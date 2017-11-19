package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 11/14/17.
  */
import com.getjenny.starchat.entities.{IndexManagementResponse, _}

import scala.concurrent.{ExecutionContext, Future}
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings._

import scala.io.Source
import java.io._

import akka.event.{Logging, LoggingAdapter}
import com.getjenny.starchat.SCActorSystem
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse
import org.elasticsearch.common.xcontent.XContentType

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Implements functions, eventually used by IndexManagementResource, for ES index management
  */
object SystemIndexManagementService {
  val elastic_client: SystemIndexManagementClient.type = SystemIndexManagementClient
  val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)
  val system_refresh_dt_json_path: String = "/index_management/json_index_spec/system/refresh_decisiontable.json"
  val user_json_path: String = "/index_management/json_index_spec/system/user.json"

  def create_index() : Future[Option[IndexManagementResponse]] = Future {
    val client: TransportClient = elastic_client.get_client()

    val system_json_is: InputStream = getClass.getResourceAsStream(system_refresh_dt_json_path)
    val user_json_is: InputStream = getClass.getResourceAsStream(user_json_path)
    val indexManagementResponse = if(system_json_is != null && user_json_is != null) {
      val system_json: String = Source.fromInputStream(system_json_is, "utf-8").mkString
      val user_json: String = Source.fromInputStream(user_json_is, "utf-8").mkString

      val system_refresh_dt_index_name = elastic_client.index_name + "." + elastic_client.system_refresh_dt_index_suffix
      val user_index_name = elastic_client.index_name + "." + elastic_client.user_index_suffix

      val create_system_index_res: CreateIndexResponse =
        client.admin().indices().prepareCreate(system_refresh_dt_index_name)
          .setSource(system_json, XContentType.JSON).get()

      val create_user_index_res: CreateIndexResponse =
        client.admin().indices().prepareCreate(user_index_name)
          .setSource(user_json, XContentType.JSON).get()

      Option {
        IndexManagementResponse("IndexCreation:" +
          " system(" + system_refresh_dt_index_name + "," + create_system_index_res.isAcknowledged.toString + ")" +
          " user(" + user_index_name + ", " + create_user_index_res.isAcknowledged.toString + ")"
        )
      }
    } else {
      val message: String = "Check one of these files: (" + system_refresh_dt_json_path + "," + user_json_path + ")"
      throw new FileNotFoundException(message)
    }
    indexManagementResponse
  }

  def remove_index() : Future[Option[IndexManagementResponse]] = Future {
    val client: TransportClient = elastic_client.get_client()

    if (! elastic_client.enable_delete_index) {
      val message: String = "operation is not allowed, contact system administrator"
      throw new Exception(message)
    }

    val system_refresh_dt_index_name = elastic_client.index_name + "." + elastic_client.system_refresh_dt_index_suffix
    val user_index_name = elastic_client.index_name + "." + elastic_client.user_index_suffix

    val system_refresh_dt_index_res: DeleteIndexResponse =
      client.admin().indices().prepareDelete(system_refresh_dt_index_name).get()

    val user_index_res: DeleteIndexResponse =
      client.admin().indices().prepareDelete(user_index_name).get()

    Option {
      IndexManagementResponse("IndexDeletion:" +
        " system(" + system_refresh_dt_index_name + "," + system_refresh_dt_index_res.isAcknowledged.toString + ")" +
        " user(" + user_index_name + "," + user_index_res.isAcknowledged.toString + ")"
      )
    }
  }

  def check_index() : Future[Option[IndexManagementResponse]] = Future {
    val client: TransportClient = elastic_client.get_client()

    val system_refresh_dt_index_name = elastic_client.index_name + "." + elastic_client.system_refresh_dt_index_suffix
    val user_index_name = elastic_client.index_name + "." + elastic_client.user_index_suffix

    val system_refresh_dt_get_mappings_req = client.admin.indices.prepareGetMappings(system_refresh_dt_index_name).get()
    val system_refresh_dt_check = system_refresh_dt_get_mappings_req
      .mappings.containsKey(system_refresh_dt_index_name)

    val user_mappings_req = client.admin.indices.prepareGetMappings(user_index_name).get()
    val user_check = user_mappings_req
      .mappings.containsKey(user_index_name)

    Option {
      IndexManagementResponse("check index: " + elastic_client.index_name
        + " system(" + system_refresh_dt_index_name + ":" + system_refresh_dt_check + ")"
        + " user(" + user_index_name + ":" + user_check + ")"
      )
    }
  }

  def update_index() : Future[Option[IndexManagementResponse]] = Future {
    val client: TransportClient = elastic_client.get_client()
    val system_refresh_dt_json_path_update: String = "/index_management/json_index_spec/system/update/refresh_decisiontable.json"
    val user_path_update: String = "/index_management/json_index_spec/system/update/user.json"

    val system_refresh_dt_json_is: InputStream = getClass.getResourceAsStream(system_refresh_dt_json_path_update)
    val user_json_is: InputStream = getClass.getResourceAsStream(user_path_update)

    val indexManagementResponse = if(system_refresh_dt_json_is != null && user_json_is != null) {
      val system_refresh_dt_json: String = Source.fromInputStream(system_refresh_dt_json_is, "utf-8").mkString
      val user_json: String = Source.fromInputStream(user_json_is, "utf-8").mkString

      val system_refresh_dt_index_name = elastic_client.index_name + "." + elastic_client.system_refresh_dt_index_suffix
      val user_index_name = elastic_client.index_name + "." + elastic_client.user_index_suffix

      val update_system_refresh_dt_index_res  =
        client.admin().indices().preparePutMapping(system_refresh_dt_index_name)
          .setType(elastic_client.system_refresh_dt_index_suffix)
          .setSource(system_refresh_dt_json, XContentType.JSON).get()

      val update_user_index_res  =
        client.admin().indices().preparePutMapping(user_index_name)
          .setType(elastic_client.user_index_suffix)
          .setSource(user_json, XContentType.JSON).get()

      Option {
        IndexManagementResponse("updated index: " + elastic_client.index_name
          + " system(" + update_system_refresh_dt_index_res.isAcknowledged.toString + ")"
          + " system(" + update_user_index_res.isAcknowledged.toString + ")"
        )
      }
    } else {
      val message: String = "Check one of these files: (" + system_refresh_dt_json_path + "," + user_json_path + ")"
      throw new FileNotFoundException(message)
    }
    indexManagementResponse
  }

  def refresh_indexes() : Future[Option[RefreshIndexResults]] = Future {
    val system_refresh_dt_index_name = elastic_client.index_name + "." + elastic_client.system_refresh_dt_index_suffix
    val user_index_name = elastic_client.index_name + "." + elastic_client.user_index_suffix

    val system_refresh_dt_index: RefreshIndexResult = elastic_client.refresh_index(system_refresh_dt_index_name)
    val user_index: RefreshIndexResult = elastic_client.refresh_index(user_index_name)

    val message = "SystemIndexManagement : Refresh : FailedShards : " + elastic_client.index_name +
      " dt_index_ack(" + system_refresh_dt_index.failed_shards_n + ")" +
      " dt_index_ack(" + user_index.failed_shards_n + ")"

    if (system_refresh_dt_index.failed_shards_n > 0 || user_index.failed_shards_n > 0) {
      throw new Exception(message)
    }

    Option {
      RefreshIndexResults(results = List(system_refresh_dt_index, user_index))
    }
  }

}
