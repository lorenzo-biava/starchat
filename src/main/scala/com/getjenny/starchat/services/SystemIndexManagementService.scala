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
import org.elasticsearch.common.xcontent.XContentType
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Implements functions, eventually used by IndexManagementResource, for ES index management
  */
object SystemIndexManagementService {
  val elastic_client = SystemIndexManagementClient
  val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)

  val system_json_path: String = "/index_management/json_index_spec/system/refresh_decisiontable.json"

  def create_index() : Future[Option[IndexManagementResponse]] = Future {
    val client: TransportClient = elastic_client.get_client()

    val system_json_is: InputStream = getClass.getResourceAsStream(system_json_path)

    val indexManagementResponse = if(system_json_path != null) {
      val system_json: String = Source.fromInputStream(system_json_is, "utf-8").mkString

      val create_index_res: CreateIndexResponse =
        client.admin().indices().prepareCreate(elastic_client.index_name)
          .addMapping(elastic_client.system_refresh_dt_type_name, system_json, XContentType.JSON)
          .get()

      Option {
        IndexManagementResponse("create index: " + elastic_client.index_name
          + " create_index_ack(" + create_index_res.isAcknowledged.toString + ")"
        )
      }
    } else {
      val message: String = "Check one of these files: (" +
        system_json_path + ", " + ")"
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

    val delete_index_req = client.admin().indices().prepareDelete(elastic_client.index_name).get()
    Option {
      IndexManagementResponse("removed index: " + elastic_client.index_name
        + " index_ack(" + delete_index_req.isAcknowledged.toString + ")"
      )
    }
  }

  def check_index() : Future[Option[IndexManagementResponse]] = Future {
    val client: TransportClient = elastic_client.get_client()

    val get_mappings_req = client.admin.indices.prepareGetMappings(elastic_client.index_name).get()

    val sys_type_check = get_mappings_req.mappings.get(elastic_client.index_name)
      .containsKey(elastic_client.system_refresh_dt_type_name)

    Option {
      IndexManagementResponse("settings index: " + elastic_client.index_name
        + " sys_type_check(" + elastic_client.system_refresh_dt_type_name + ":" + sys_type_check + ")"
      )
    }
  }

  def update_index() : Future[Option[IndexManagementResponse]] = Future {
    val client: TransportClient = elastic_client.get_client()

    val system_json_is: InputStream = getClass.getResourceAsStream(system_json_path)

    val indexManagementResponse = if(system_json_is != null) {
      val system_json: String = Source.fromInputStream(system_json_is, "utf-8").mkString

      val update_sys_type_res  =
        client.admin().indices().preparePutMapping(elastic_client.index_name)
          .setType(elastic_client.system_refresh_dt_type_name).setSource(system_json, XContentType.JSON).get()

      Option {
        IndexManagementResponse("updated index: " + elastic_client.index_name
          + " sys_type_ack(" + update_sys_type_res.isAcknowledged.toString + ")"
        )
      }
    } else {
      val message: String = "Check one of these files: (" +
        system_json_path + ")"
      throw new FileNotFoundException(message)
    }
    indexManagementResponse
  }

  def refresh_index() : Future[Option[RefreshIndexResult]] = Future {
    val refresh_index: RefreshIndexResult = elastic_client.refresh_index(elastic_client.index_name)
    if (refresh_index.failed_shards_n > 0) {
      throw new Exception("SystemIndexManagement : index refresh failed: (" + elastic_client.index_name + ")")
    }
    Option { refresh_index }
  }

}