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
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse
import org.elasticsearch.common.xcontent.XContentType

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Implements functions, eventually used by IndexManagementResource, for ES index management
  */
object IndexManagementService {
  val elastic_client: IndexManagementClient.type = IndexManagementClient
  val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)

  val state_json_path: String = "/index_management/json_index_spec/general/state.json"
  val question_json_path: String = "/index_management/json_index_spec/general/question.json"
  val term_json_path: String = "/index_management/json_index_spec/general/term.json"

  def create_index(index_name: String, language: String) : Future[Option[IndexManagementResponse]] = Future {
    val client: TransportClient = elastic_client.get_client()

    val analyzer_json_path: String = "/index_management/json_index_spec/" + language + "/analyzer.json"

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

      val dt_index_name = index_name + "." + elastic_client.dt_index_suffix
      val kb_index_name = index_name + "." + elastic_client.kb_index_suffix
      val term_index_name = index_name + "." + elastic_client.term_index_suffix

      val create_dt_index_res: CreateIndexResponse =
        client.admin().indices().prepareCreate(dt_index_name)
          .setSettings(Settings.builder().loadFromSource(analyzer_json, XContentType.JSON))
          .setSource(state_json, XContentType.JSON).get()

      val create_kb_index_res: CreateIndexResponse =
        client.admin().indices().prepareCreate(kb_index_name)
          .setSettings(Settings.builder().loadFromSource(analyzer_json, XContentType.JSON))
          .setSource(question_json, XContentType.JSON).get()

      val create_term_index_res: CreateIndexResponse =
        client.admin().indices().prepareCreate(term_index_name)
          .setSettings(Settings.builder().loadFromSource(analyzer_json, XContentType.JSON))
          .setSource(term_json, XContentType.JSON).get()

      Option {
        IndexManagementResponse("IndexCreation:" +
          " decisiontable(" + dt_index_name + "," + create_dt_index_res.isAcknowledged.toString + ")" +
          " knowledgebase(" + kb_index_name + "," + create_kb_index_res.isAcknowledged.toString + ")" +
          " term(" + term_index_name + "," + create_term_index_res.isAcknowledged.toString + ")"
        )
      }
    } else {
      val message: String = "Check one of these files: (" + analyzer_json_path + ", " +
        state_json_path + ", " + question_json_path + ", " + term_json_path + ")"
      throw new FileNotFoundException(message)
    }
    indexManagementResponse
  }
  
  def remove_index(index_name: String) : Future[Option[IndexManagementResponse]] = Future {
    val client: TransportClient = elastic_client.get_client()

    if (! elastic_client.enable_delete_index) {
      val message: String = "operation is not allowed, contact system administrator"
      throw new Exception(message)
    }

    val dt_index_name = index_name + "." + elastic_client.dt_index_suffix
    val kb_index_name = index_name + "." + elastic_client.kb_index_suffix
    val term_index_name = index_name + "." + elastic_client.term_index_suffix

    val delete_dt_index_res: DeleteIndexResponse =
      client.admin().indices().prepareDelete(dt_index_name).get()

    val delete_kb_index_res: DeleteIndexResponse =
      client.admin().indices().prepareDelete(kb_index_name).get()

    val delete_term_index_res: DeleteIndexResponse =
      client.admin().indices().prepareDelete(term_index_name).get()

    Option {
      IndexManagementResponse("IndexDeletion:" +
        " decisiontable(" + dt_index_name + "," + delete_dt_index_res.isAcknowledged.toString + ")" +
        " knowledgebase(" + kb_index_name + "," + delete_kb_index_res.isAcknowledged.toString + ")" +
        " term(" + term_index_name + "," + delete_term_index_res.isAcknowledged.toString + ")"
      )
    }
  }

  def check_index(index_name: String) : Future[Option[IndexManagementResponse]] = Future {
    val client: TransportClient = elastic_client.get_client()

    val dt_index_name = index_name + "." + elastic_client.dt_index_suffix
    val kb_index_name = index_name + "." + elastic_client.kb_index_suffix
    val term_index_name = index_name + "." + elastic_client.term_index_suffix

    val dt_get_mappings_req = client.admin.indices.prepareGetMappings(dt_index_name).get()
    val dt_check = dt_get_mappings_req.mappings.containsKey(dt_index_name)
    val kb_get_mappings_req = client.admin.indices.prepareGetMappings(kb_index_name).get()
    val kb_check = kb_get_mappings_req.mappings.containsKey(kb_index_name)
    val term_get_mappings_req = client.admin.indices.prepareGetMappings(term_index_name).get()
    val term_check = term_get_mappings_req.mappings.containsKey(term_index_name)

    Option {
      IndexManagementResponse("settings index: " + index_name
        + " dt_type_check(" + dt_index_name + ":" + dt_check + ")"
        + " kb_type_check(" + kb_index_name + ":" + kb_check + ")"
        + " term_type_name(" + term_index_name + ":" + term_check + ")"
      )
    }
  }

  def update_index(index_name: String, language: String) : Future[Option[IndexManagementResponse]] = Future {
    val client: TransportClient = elastic_client.get_client()

    val analyzer_json_path: String = "/index_management/json_index_spec/" + language + "/analyzer.json"
    val analyzer_json_is: InputStream = getClass.getResourceAsStream(analyzer_json_path)
    val state_json_is: InputStream = getClass.getResourceAsStream(state_json_path)
    val question_json_is: InputStream = getClass.getResourceAsStream(question_json_path)
    val term_json_is: InputStream = getClass.getResourceAsStream(term_json_path)

    val indexManagementResponse = if(analyzer_json_is != null &&
      state_json_is != null && question_json_is != null && term_json_is != null) {
      val state_json: String = Source.fromInputStream(state_json_is, "utf-8").mkString
      val question_json: String = Source.fromInputStream(question_json_is, "utf-8").mkString
      val term_json: String = Source.fromInputStream(term_json_is, "utf-8").mkString

      val dt_index_name = index_name + "." + elastic_client.dt_index_suffix
      val kb_index_name = index_name + "." + elastic_client.kb_index_suffix
      val term_index_name = index_name + "." + elastic_client.term_index_suffix

      val update_dt_index_res  =
        client.admin().indices().preparePutMapping(dt_index_name)
          .setSource(state_json, XContentType.JSON).get()

      val update_kb_index_res  =
        client.admin().indices().preparePutMapping(kb_index_name)
          .setSource(question_json, XContentType.JSON).get()

      val update_term_index_res  =
        client.admin().indices().preparePutMapping(term_index_name)
          .setSource(term_json, XContentType.JSON).get()

      Option {
        IndexManagementResponse("updated index: " + index_name
          + " dt_index_ack(" + update_dt_index_res.isAcknowledged.toString + ")"
          + " kb_index_ack(" + update_kb_index_res.isAcknowledged.toString + ")"
          + " kb_index_ack(" + update_term_index_res.isAcknowledged.toString + ")"
        )
      }
    } else {
      val message: String = "Check one of these files: (" +
        analyzer_json_path + ", " + state_json_path + ", " + question_json_path + ")"
      throw new FileNotFoundException(message)
    }
    indexManagementResponse
  }

  def refresh_indexes(index_name: String) : Future[Option[RefreshIndexResults]] = Future {
    val dt_index_name = index_name + "." + elastic_client.dt_index_suffix
    val kb_index_name = index_name + "." + elastic_client.kb_index_suffix
    val term_index_name = index_name + "." + elastic_client.term_index_suffix

    val dt_refresh_index: RefreshIndexResult = elastic_client.refresh_index(dt_index_name)
    val kb_refresh_index: RefreshIndexResult = elastic_client.refresh_index(kb_index_name)
    val term_refresh_index: RefreshIndexResult = elastic_client.refresh_index(term_index_name)

    val message = "IndexManagement : Refresh : FailedShards : " + index_name +
      " dt_index_ack(" + dt_refresh_index.failed_shards_n + ")" +
      " kb_index_ack(" + kb_refresh_index.failed_shards_n + ")" +
      " kb_index_ack(" + term_refresh_index.failed_shards_n + ")"

    if (dt_refresh_index.failed_shards_n > 0 || kb_refresh_index.failed_shards_n > 0 ||
      term_refresh_index.failed_shards_n > 0) {
      throw new Exception(message)
    }

    Option {
      RefreshIndexResults(results = List(dt_refresh_index, kb_refresh_index, term_refresh_index))
    }
  }

}
