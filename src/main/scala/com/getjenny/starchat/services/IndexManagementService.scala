package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 10/03/17.
  */

import java.io._

import akka.event.{Logging, LoggingAdapter}
import com.getjenny.starchat.SCActorSystem
import com.getjenny.starchat.entities._
import com.getjenny.starchat.services.esclient.IndexManagementElasticClient
import com.getjenny.starchat.utils.Index
import org.elasticsearch.action.admin.indices.close.CloseIndexResponse
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse
import org.elasticsearch.action.admin.indices.open.OpenIndexResponse
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings._
import org.elasticsearch.common.xcontent.XContentType
import scalaz.Scalaz._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source

case class IndexManagementServiceException(message: String = "", cause: Throwable = None.orNull)
  extends Exception(message, cause)

/**
  * Implements functions, eventually used by IndexManagementResource, for ES index management
  */
object IndexManagementService {
  private[this] val elasticClient: IndexManagementElasticClient.type = IndexManagementElasticClient
  private[this] val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)

  private[this] def analyzerFiles(language: String): JsonMappingAnalyzersIndexFiles =
    JsonMappingAnalyzersIndexFiles(path = "/index_management/json_index_spec/" + language + "/analyzer.json",
      updatePath = "/index_management/json_index_spec/" + language + "/update/analyzer.json",
      indexSuffix = "")

  private[this] val schemaFiles: List[JsonMappingAnalyzersIndexFiles] = List[JsonMappingAnalyzersIndexFiles](
    JsonMappingAnalyzersIndexFiles(path = "/index_management/json_index_spec/general/state.json",
      updatePath = "/index_management/json_index_spec/general/update/state.json",
      indexSuffix = elasticClient.dtIndexSuffix),
    JsonMappingAnalyzersIndexFiles(path = "/index_management/json_index_spec/general/question_answer.json",
      updatePath = "/index_management/json_index_spec/general/update/question_answer.json",
      indexSuffix = elasticClient.convLogsIndexSuffix),
    JsonMappingAnalyzersIndexFiles(path = "/index_management/json_index_spec/general/question_answer.json",
      updatePath = "/index_management/json_index_spec/general/update/question_answer.json",
      indexSuffix = elasticClient.priorDataIndexSuffix),
    JsonMappingAnalyzersIndexFiles(path = "/index_management/json_index_spec/general/question_answer.json",
      updatePath = "/index_management/json_index_spec/general/update/question_answer.json",
      indexSuffix = elasticClient.kbIndexSuffix),
    JsonMappingAnalyzersIndexFiles(path = "/index_management/json_index_spec/general/term.json",
      updatePath = "/index_management/json_index_spec/general/update/term.json",
      indexSuffix = elasticClient.termIndexSuffix)
  )

  def create(indexName: String,
             indexSuffix: Option[String] = None) : Future[Option[IndexManagementResponse]] = Future {
    val client: TransportClient = elasticClient.client

    // extract language from index name
    val indexLanguageRegex = "^(?:(index)_([a-z]{1,256})_([A-Za-z0-9_]{1,256}))$".r

    val (_, language, _) = indexName match {
      case indexLanguageRegex(indexPattern, languagePattern, arbitraryPattern) =>
        (indexPattern, languagePattern, arbitraryPattern)
      case _ => throw new Exception("index name is not well formed")
    }

    val analyzerJsonPath: String = analyzerFiles(language).path
    val analyzerJsonIs: Option[InputStream] = Option{getClass.getResourceAsStream(analyzerJsonPath)}
    val analyzerJson: String = analyzerJsonIs match {
      case Some(stream) => Source.fromInputStream(stream, "utf-8").mkString
      case _ =>
        val message = "Check the file: (" + analyzerJsonPath + ")"
        throw new FileNotFoundException(message)
    }

    val operationsMessage: List[String] = schemaFiles.filter(item => {
      indexSuffix match {
        case Some(t) => t === item.indexSuffix
        case _ => true
      }
    }).map(item => {
      val jsonInStream: Option[InputStream] = Option{getClass.getResourceAsStream(item.path)}

      val schemaJson: String = jsonInStream match {
        case Some(stream) => Source.fromInputStream(stream, "utf-8").mkString
        case _ =>
          val message = "Check the file: (" + item.path + ")"
          throw new FileNotFoundException(message)
      }

      val fullIndexName = indexName + "." + item.indexSuffix

      val createIndexRes: CreateIndexResponse =
        client.admin().indices().prepareCreate(fullIndexName)
          .setSettings(Settings.builder().loadFromSource(analyzerJson, XContentType.JSON))
          .setSource(schemaJson, XContentType.JSON).get()

      item.indexSuffix + "(" + fullIndexName + ", " + createIndexRes.isAcknowledged.toString + ")"
    })

    val message = "IndexCreation: " + operationsMessage.mkString(" ")

    Option { IndexManagementResponse(message) }
  }

  def remove(indexName: String,
             indexSuffix: Option[String] = None) : Future[Option[IndexManagementResponse]] = Future {
    val client: TransportClient = elasticClient.client

    if (! elasticClient.enableDeleteIndex) {
      val message: String = "operation is not allowed, contact system administrator"
      throw IndexManagementServiceException(message)
    }

    val operationsMessage: List[String] = schemaFiles.filter(item => {
      indexSuffix match {
        case Some(t) => t === item.indexSuffix
        case _ => true
      }
    }).map(item => {
      val fullIndexName = indexName + "." + item.indexSuffix

      val deleteIndexRes: DeleteIndexResponse =
        client.admin().indices().prepareDelete(fullIndexName).get()

      item.indexSuffix + "(" + fullIndexName + ", " + deleteIndexRes.isAcknowledged.toString + ")"

    })

    val message = "IndexDeletion: " + operationsMessage.mkString(" ")

    Option { IndexManagementResponse(message) }
  }

  def check(indexName: String,
            indexSuffix: Option[String] = None) : Future[Option[IndexManagementResponse]] = Future {
    val client: TransportClient = elasticClient.client

    val operationsMessage: List[String] = schemaFiles.filter(item => {
      indexSuffix match {
        case Some(t) => t === item.indexSuffix
        case _ => true
      }
    }).map(item => {
      val fullIndexName = indexName + "." + item.indexSuffix
      val getMappingsReq = client.admin.indices.prepareGetMappings(fullIndexName).get()
      val check = getMappingsReq.mappings.containsKey(fullIndexName)
      item.indexSuffix + "(" + fullIndexName + ", " + check + ")"
    })

    val message = "IndexCheck: " + operationsMessage.mkString(" ")

    Option { IndexManagementResponse(message) }
  }

  def openClose(indexName: String, indexSuffix: Option[String] = None,
                operation: String): Future[List[OpenCloseIndex]] = Future {
    val client: TransportClient = elasticClient.client
    schemaFiles.filter(item => {
      indexSuffix match {
        case Some(t) => t === item.indexSuffix
        case _ => true
      }
    }).map(item => {
      val fullIndexName = indexName + "." + item.indexSuffix
      operation match {
        case "close" =>
          val closeIndexResponse: CloseIndexResponse = client.admin().indices().prepareClose(fullIndexName).get()
          OpenCloseIndex(indexName = indexName, indexSuffix = item.indexSuffix, fullIndexName = fullIndexName,
            operation = operation, acknowledgement = closeIndexResponse.isAcknowledged)
        case "open" =>
          val openIndexResponse: OpenIndexResponse = client.admin().indices().prepareOpen(fullIndexName).get()
          OpenCloseIndex(indexName = indexName, indexSuffix = item.indexSuffix, fullIndexName = fullIndexName,
            operation = operation, acknowledgement = openIndexResponse.isAcknowledged)
        case _ => throw IndexManagementServiceException("operation not supported on index: " + operation)
      }
    })
  }

  def updateSettings(indexName: String,
                     indexSuffix: Option[String] = None) : Future[Option[IndexManagementResponse]] = Future {
    val client: TransportClient = elasticClient.client

    val (orgName, language, arbitrary) = Index.patternsFromIndex(indexName: String)

    val analyzerJsonPath: String = analyzerFiles(language).updatePath
    val analyzerJsonIs: Option[InputStream] = Option{getClass.getResourceAsStream(analyzerJsonPath)}
    val analyzerJson: String = analyzerJsonIs match {
      case Some(stream) => Source.fromInputStream(stream, "utf-8").mkString
      case _ =>
        val message = "file not found: (" + analyzerJsonPath + ")"
        throw new FileNotFoundException(message)
    }

    val operationsMessage: List[String] = schemaFiles.filter(item => {
      indexSuffix match {
        case Some(t) => t === item.indexSuffix
        case _ => true
      }
    }).map(item => {
      val fullIndexName = indexName + "." + item.indexSuffix
      val updateIndexRes =
        client.admin().indices().prepareUpdateSettings().setIndices(fullIndexName)
          .setSettings(Settings.builder().loadFromSource(analyzerJson, XContentType.JSON)).get()
      item.indexSuffix + "(" + fullIndexName + ", " + updateIndexRes.isAcknowledged.toString + ")"
    })

    val message = "IndexSettingsUpdate: " + operationsMessage.mkString(" ")

    Option { IndexManagementResponse(message) }
  }

  def updateMappings(indexName: String,
                     indexSuffix: Option[String] = None) : Future[Option[IndexManagementResponse]] = Future {
    val client: TransportClient = elasticClient.client

    val (orgName, language, arbitrary) = Index.patternsFromIndex(indexName: String)

    val operationsMessage: List[String] = schemaFiles.filter(item => {
      indexSuffix match {
        case Some(t) => t === item.indexSuffix
        case _ => true
      }
    }).map(item => {
      val jsonInStream: Option[InputStream] = Option{getClass.getResourceAsStream(item.updatePath)}

      val schemaJson: String = jsonInStream match {
        case Some(stream) => Source.fromInputStream(stream, "utf-8").mkString
        case _ =>
          val message = "Check the file: (" + item.updatePath + ")"
          throw new FileNotFoundException(message)
      }

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

  def refresh(indexName: String,
              indexSuffix: Option[String] = None) : Future[Option[RefreshIndexResults]] = Future {
    val operationsResults: List[RefreshIndexResult] = schemaFiles.filter(item => {
      indexSuffix match {
        case Some(t) => t === item.indexSuffix
        case _ => true
      }
    }).map(item => {
      val fullIndexName = indexName + "." + item.indexSuffix
      val refreshIndexRes: RefreshIndexResult = elasticClient.refresh(fullIndexName)
      if (refreshIndexRes.failed_shards_n > 0) {
        val indexRefreshMessage = item.indexSuffix + "(" + fullIndexName + ", " +
          refreshIndexRes.failed_shards_n + ")"
        throw new Exception(indexRefreshMessage)
      }

      refreshIndexRes
    })

    Option { RefreshIndexResults(results = operationsResults) }
  }

}


