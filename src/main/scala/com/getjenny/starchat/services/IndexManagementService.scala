package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 10/03/17.
  */

import com.getjenny.starchat.entities._

import scala.concurrent.Future
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
  val elasticClient: IndexManagementClient.type = IndexManagementClient
  val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)

  val schemaFiles: List[JsonIndexFiles] = List[JsonIndexFiles](
    JsonIndexFiles(path = "/index_management/json_index_spec/general/state.json",
      updatePath = "/index_management/json_index_spec/general/update/state.json",
      indexSuffix = elasticClient.dtIndexSuffix),
    JsonIndexFiles(path = "/index_management/json_index_spec/general/question.json",
      updatePath = "/index_management/json_index_spec/general/update/question.json",
      indexSuffix = elasticClient.kbIndexSuffix),
    JsonIndexFiles(path = "/index_management/json_index_spec/general/term.json",
      updatePath = "/index_management/json_index_spec/general/update/term.json",
      indexSuffix = elasticClient.termIndexSuffix)
  )

  def createIndex(indexName: String) : Future[Option[IndexManagementResponse]] = Future {
    val client: TransportClient = elasticClient.getClient()

    // extract language from index name
    val indexLanguageRegex = "^(?:(index)_([a-z]{1,256})_([A-Za-z0-9_]{1,256}))$".r
    val indexPatterns = indexName match {
      case indexLanguageRegex(index_pattern, language_pattern, arbitrary_pattern) =>
        (index_pattern, language_pattern, arbitrary_pattern)
      case _ => throw new Exception("index name is not well formed")
    }
    val language: String = indexPatterns._2

    val analyzerJsonPath: String = "/index_management/json_index_spec/" + language + "/analyzer.json"
    val analyzerJsonIs: InputStream = getClass.getResourceAsStream(analyzerJsonPath)
    val analyzerJson: String = Source.fromInputStream(analyzerJsonIs, "utf-8").mkString

    if(analyzerJsonIs == None.orNull) {
      val message = "Check the file: (" + analyzerJsonPath + ")"
      throw new FileNotFoundException(message)
    }

    val operations_message: List[String] = schemaFiles.map(item => {
      val jsonInStream: InputStream = getClass.getResourceAsStream(item.path)
      if (jsonInStream == None.orNull) {
        val message = "Check the file: (" + item.path + ")"
        throw new FileNotFoundException(message)
      }

      val schemaJson: String = Source.fromInputStream(jsonInStream, "utf-8").mkString
      val fullIndexName = indexName + "." + item.indexSuffix

      val createIndexRes: CreateIndexResponse =
        client.admin().indices().prepareCreate(fullIndexName)
          .setSettings(Settings.builder().loadFromSource(analyzerJson, XContentType.JSON))
          .setSource(schemaJson, XContentType.JSON).get()

      item.indexSuffix + "(" + fullIndexName + ", " + createIndexRes.isAcknowledged.toString + ")"
    })

    val message = "IndexCreation: " + operations_message.mkString(" ")

    Option { IndexManagementResponse(message) }
  }

  def removeIndex(indexName: String) : Future[Option[IndexManagementResponse]] = Future {
    val client: TransportClient = elasticClient.getClient()

    if (! elasticClient.enableDeleteIndex) {
      val message: String = "operation is not allowed, contact system administrator"
      throw new Exception(message)
    }

    val operationsMessage: List[String] = schemaFiles.map(item => {
      val fullIndexName = indexName + "." + item.indexSuffix

      val deleteIndexRes: DeleteIndexResponse =
        client.admin().indices().prepareDelete(fullIndexName).get()

      item.indexSuffix + "(" + fullIndexName + ", " + deleteIndexRes.isAcknowledged.toString + ")"

    })

    val message = "IndexDeletion: " + operationsMessage.mkString(" ")

    Option { IndexManagementResponse(message) }
  }

  def checkIndex(indexName: String) : Future[Option[IndexManagementResponse]] = Future {
    val client: TransportClient = elasticClient.getClient()

    val operationsMessage: List[String] = schemaFiles.map(item => {
      val fullIndexName = indexName + "." + item.indexSuffix
      val getMappingsReq = client.admin.indices.prepareGetMappings(fullIndexName).get()
      val check = getMappingsReq.mappings.containsKey(fullIndexName)
      item.indexSuffix + "(" + fullIndexName + ", " + check + ")"
    })

    val message = "IndexCheck: " + operationsMessage.mkString(" ")

    Option { IndexManagementResponse(message) }
  }

  def updateIndex(indexName: String, language: String) : Future[Option[IndexManagementResponse]] = Future {
    val client: TransportClient = elasticClient.getClient()

    val analyzerJsonPath: String = "/index_management/json_index_spec/" + language + "/analyzer.json"
    val analyzerJsonIs: InputStream = getClass.getResourceAsStream(analyzerJsonPath)
    val analyzerJson: String = Source.fromInputStream(analyzerJsonIs, "utf-8").mkString

    if(analyzerJsonIs == None.orNull) {
      val message = "Check the file: (" + analyzerJsonPath + ")"
      throw new FileNotFoundException(message)
    }

    val operationsMessage: List[String] = schemaFiles.map(item => {
      val jsonInStream: InputStream = getClass.getResourceAsStream(item.updatePath)

      if (jsonInStream == None.orNull) {
        val message = "Check the file: (" + item.path + ")"
        throw new FileNotFoundException(message)
      }

      val schemaJson: String = Source.fromInputStream(jsonInStream, "utf-8").mkString
      val fullIndexName = indexName + "." + item.indexSuffix

      val updateIndexRes  =
        client.admin().indices().preparePutMapping().setIndices(fullIndexName)
          .setType(item.indexSuffix)
          .setSource(schemaJson, XContentType.JSON).get()

      item.indexSuffix + "(" + fullIndexName + ", " + updateIndexRes.isAcknowledged.toString + ")"
    })

    val message = "IndexUpdate: " + operationsMessage.mkString(" ")

    Option { IndexManagementResponse(message) }
  }

  def refreshIndexes(indexName: String) : Future[Option[RefreshIndexResults]] = Future {
    val client: TransportClient = elasticClient.getClient()

    val operationsResults: List[RefreshIndexResult] = schemaFiles.map(item => {
      val fullIndexName = indexName + "." + item.indexSuffix
      val refreshIndexRes: RefreshIndexResult = elasticClient.refreshIndex(fullIndexName)
      if (refreshIndexRes.failed_shards_n > 0) {
        val indexRefreshMessage = item.indexSuffix + "(" + fullIndexName + ", " + refreshIndexRes.failed_shards_n + ")"
        throw new Exception(indexRefreshMessage)
      }

      refreshIndexRes
    })

    Option { RefreshIndexResults(results = operationsResults) }
  }

}


