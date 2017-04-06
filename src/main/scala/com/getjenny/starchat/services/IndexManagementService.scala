package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 10/03/17.
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

/**
  * Implements functions, eventually used by IndexManagementResource, for ES index management
  */
class IndexManagementService(implicit val executionContext: ExecutionContext) {
  val elastic_client = IndexManagementClient
  val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)

  val lang: String = elastic_client.index_language
  val analyzer_json_path: String = "/index_management/json_index_spec/" + lang + "/analyzer.json"
  val state_json_path: String = "/index_management/json_index_spec/general/state.json"
  val question_json_path: String = "/index_management/json_index_spec/general/question.json"
  val term_json_path: String = "/index_management/json_index_spec/general/term.json"

  def create_index() : Option[IndexManagementResponse] = {
    val client: TransportClient = elastic_client.get_client()

    val analyzer_json_is: InputStream = getClass.getResourceAsStream(analyzer_json_path)
    val state_json_is: InputStream = getClass.getResourceAsStream(state_json_path)
    val question_json_is: InputStream = getClass.getResourceAsStream(question_json_path)
    val term_json_is: InputStream = getClass.getResourceAsStream(term_json_path)

    val indexManagementResponse = if(analyzer_json_is != null &&
      state_json_is != null && question_json_is != null && term_json_is != null) {
      val analyzer_json: String = Source.fromInputStream(analyzer_json_is, "utf-8").mkString
      val state_json: String = Source.fromInputStream(state_json_is, "utf-8").mkString
      val question_json: String = Source.fromInputStream(question_json_is, "utf-8").mkString
      val term_json: String = Source.fromInputStream(term_json_is, "utf-8").mkString

      val create_index_res: CreateIndexResponse =
        client.admin().indices().prepareCreate(elastic_client.index_name)
          .setSettings(Settings.builder().loadFromSource(analyzer_json, XContentType.JSON))
          .addMapping(elastic_client.dt_type_name, state_json, XContentType.JSON)
          .addMapping(elastic_client.kb_type_name, question_json, XContentType.JSON)
          .addMapping(elastic_client.term_type_name, term_json, XContentType.JSON)
          .get()

      Option {
        IndexManagementResponse("create index: " + elastic_client.index_name
          + " create_index_ack(" + create_index_res.isAcknowledged.toString + ")"
        )
      }
    } else {
      val message: String = "Check one of these files: (" +
        analyzer_json_path + ", " + state_json_path + ", " + question_json_path + ", " + term_json_path + ")"
      throw new FileNotFoundException(message)
    }
    indexManagementResponse
  }
  
  def remove_index() : Option[IndexManagementResponse] = {
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

  def check_index() : Option[IndexManagementResponse] = {
    val client: TransportClient = elastic_client.get_client()

    val get_mappings_req = client.admin.indices.prepareGetMappings(elastic_client.index_name).get()

    val dt_type_check = get_mappings_req.mappings.get(elastic_client.index_name)
      .containsKey(elastic_client.dt_type_name)
    val kb_type_check = get_mappings_req.mappings.get(elastic_client.index_name)
      .containsKey(elastic_client.kb_type_name)
    val term_type_check = get_mappings_req.mappings.get(elastic_client.index_name)
      .containsKey(elastic_client.kb_type_name)

    Option {
      IndexManagementResponse("settings index: " + elastic_client.index_name
        + " dt_type_check(" + elastic_client.dt_type_name + ":" + dt_type_check + ")"
        + " kb_type_check(" + elastic_client.kb_type_name + ":" + kb_type_check + ")"
        + " term_type_name(" + elastic_client.term_type_name + ":" + term_type_check + ")"
      )
    }
  }

  def update_index() : Option[IndexManagementResponse] = {
    val client: TransportClient = elastic_client.get_client()

    val analyzer_json_is: InputStream = getClass.getResourceAsStream(analyzer_json_path)
    val state_json_is: InputStream = getClass.getResourceAsStream(state_json_path)
    val question_json_is: InputStream = getClass.getResourceAsStream(question_json_path)
    val term_json_is: InputStream = getClass.getResourceAsStream(term_json_path)

    val indexManagementResponse = if(analyzer_json_is != null &&
      state_json_is != null && question_json_is != null && term_json_is != null) {
      val state_json: String = Source.fromInputStream(state_json_is, "utf-8").mkString
      val question_json: String = Source.fromInputStream(question_json_is, "utf-8").mkString
      val term_json: String = Source.fromInputStream(term_json_is, "utf-8").mkString

      val update_dt_type_res  =
        client.admin().indices().preparePutMapping(elastic_client.index_name)
          .setType(elastic_client.dt_type_name).setSource(state_json, XContentType.JSON).get()

      val update_kb_type_res  =
        client.admin().indices().preparePutMapping(elastic_client.index_name)
          .setType(elastic_client.kb_type_name).setSource(question_json, XContentType.JSON).get()

      val update_term_type_res  =
        client.admin().indices().preparePutMapping(elastic_client.index_name)
          .setType(elastic_client.kb_type_name).setSource(term_json, XContentType.JSON).get()

      Option {
        IndexManagementResponse("updated index: " + elastic_client.index_name
          + " dt_type_ack(" + update_dt_type_res.isAcknowledged.toString + ")"
          + " kb_type_ack(" + update_kb_type_res.isAcknowledged.toString + ")"
          + " kb_type_ack(" + update_term_type_res.isAcknowledged.toString + ")"
        )
      }
    } else {
      val message: String = "Check one of these files: (" +
        analyzer_json_path + ", " + state_json_path + ", " + question_json_path + ")"
      throw new FileNotFoundException(message)
    }
    indexManagementResponse
  }

  def refresh_index() : Option[RefreshIndexResult] = {
    val refresh_index: RefreshIndexResult = elastic_client.refresh_index()
    if (refresh_index.failed_shards_n > 0) {
      throw new Exception("IndexManagement : index refresh failed: (" + elastic_client.index_name + ")")
    }
    Option { refresh_index }
  }

}
