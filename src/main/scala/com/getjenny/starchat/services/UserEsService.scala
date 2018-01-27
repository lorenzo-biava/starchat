package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 01/12/17.
  */

import akka.event.{Logging, LoggingAdapter}
import com.getjenny.starchat.entities._
import com.getjenny.starchat.services.auth.{AuthenticatorException, AbstractStarChatAuthenticator}
import com.getjenny.starchat.SCActorSystem
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import javax.naming.AuthenticationException

import org.elasticsearch.action.delete.{DeleteRequestBuilder, DeleteResponse}
import org.elasticsearch.action.get.GetRequestBuilder
import org.elasticsearch.action.get.{GetResponse, MultiGetItemResponse, MultiGetRequestBuilder, MultiGetResponse}
import org.elasticsearch.action.update.UpdateResponse
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.xcontent.XContentBuilder
import org.elasticsearch.common.xcontent.XContentFactory._
import org.elasticsearch.rest.RestStatus
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import com.getjenny.analyzer.util.RandomNumbers

/**
  * Implements functions, eventually used by IndexManagementResource, for ES index management
  */
class UserEsService extends AbstractUserService {
  val config: Config = ConfigFactory.load()
  val elasticClient: SystemIndexManagementElasticClient.type = SystemIndexManagementElasticClient
  val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)
  val indexName: String = elasticClient.indexName + "." + elasticClient.userIndexSuffix

  val admin: String = config.getString("starchat.basic_http_es.admin")
  val password: String = config.getString("starchat.basic_http_es.password")
  val salt: String = config.getString("starchat.basic_http_es.salt")
  val admin_user = User(id = admin, password = password, salt = salt,
    permissions = Map("admin" -> Set(Permissions.admin)))

  def create(user: User): Future[IndexDocumentResult] = Future {

    if(user.id == "admin") {
      throw new AuthenticationException("admin user cannot be changed")
    }

    val builder : XContentBuilder = jsonBuilder().startObject()

    builder.field("id", user.id)
    builder.field("password", user.password)
    builder.field("salt", user.salt)

    val permissions = builder.startObject("permissions")
    user.permissions.foreach(x => {
      val array = permissions.field(x._1).startArray()
      x._2.foreach(p => { array.value(p)})
      array.endArray()
    })
    permissions.endObject()

    builder.endObject()

    val client: TransportClient = elasticClient.getClient()
    val response = client.prepareIndex().setIndex(indexName)
      .setCreate(true)
      .setType(elasticClient.userIndexSuffix)
      .setId(user.id)
      .setSource(builder).get()

    val refresh_index = elasticClient.refreshIndex(indexName)
    if(refresh_index.failed_shards_n > 0) {
      throw new Exception("User : index refresh failed: (" + indexName + ")")
    }

    val doc_result: IndexDocumentResult = IndexDocumentResult(index = response.getIndex,
      dtype = response.getType,
      id = response.getId,
      version = response.getVersion,
      created = response.status == RestStatus.CREATED
    )

    doc_result
  }

  def update(id: String, user: UserUpdate):
  Future[UpdateDocumentResult] = Future {

    if(id == "admin") {
      throw new AuthenticationException("admin user cannot be changed")
    }

    val builder : XContentBuilder = jsonBuilder().startObject()

    user.password match {
      case Some(t) => builder.field("password", t)
      case None => ;
    }

    user.salt match {
      case Some(t) => builder.field("salt", t)
      case None => ;
    }

    user.permissions match {
      case Some(t) =>
        val permissions = builder.startObject("permissions")
        user.permissions.getOrElse(Map.empty).foreach(x => {
          val array = permissions.field(x._1).startArray()
          x._2.foreach(p => { array.value(p)})
          array.endArray()
        })
        permissions.endObject()
      case None => ;
    }

    builder.endObject()

    val client: TransportClient = elasticClient.getClient()
    val response: UpdateResponse = client.prepareUpdate().setIndex(indexName)
      .setType(elasticClient.userIndexSuffix).setId(id)
      .setDoc(builder)
      .get()

    val refresh_index = elasticClient.refreshIndex(indexName)
    if(refresh_index.failed_shards_n > 0) {
      throw new Exception("User : index refresh failed: (" + indexName + ")")
    }

    val doc_result: UpdateDocumentResult = UpdateDocumentResult(index = response.getIndex,
      dtype = response.getType,
      id = response.getId,
      version = response.getVersion,
      created = response.status == RestStatus.CREATED
    )

    doc_result
  }

  def delete(id: String): Future[DeleteDocumentResult] = Future {

    if(id == "admin") {
      throw new AuthenticationException("admin user cannot be changed")
    }

    val client: TransportClient = elasticClient.getClient()
    val response: DeleteResponse = client.prepareDelete().setIndex(indexName)
      .setType(elasticClient.userIndexSuffix).setId(id).get()

    val refresh_index = elasticClient.refreshIndex(indexName)
    if(refresh_index.failed_shards_n > 0) {
      throw new Exception("User: index refresh failed: (" + indexName + ")")
    }

    val doc_result: DeleteDocumentResult = DeleteDocumentResult(index = response.getIndex,
      dtype = response.getType,
      id = response.getId,
      version = response.getVersion,
      found = response.status != RestStatus.NOT_FOUND
    )

    doc_result
  }

  def read(id: String): Future[User] = Future {
    if(id == "admin") {
      admin_user
    } else {

      val client: TransportClient = elasticClient.getClient()
      val getBuilder: GetRequestBuilder = client.prepareGet(indexName, elasticClient.userIndexSuffix, id)

      val response: GetResponse = getBuilder.get()
      val source = response.getSource.asScala.toMap

      val userId: String = source.get("id") match {
        case Some(t) => t.asInstanceOf[String]
        case None => ""
      }

      val password: String = source.get("password") match {
        case Some(t) => t.asInstanceOf[String]
        case None => ""
      }

      val salt: String = source.get("salt") match {
        case Some(t) => t.asInstanceOf[String]
        case None => ""
      }

      val permissions: Map[String, Set[Permissions.Value]] = source.get("permissions") match {
        case Some(t) => t.asInstanceOf[java.util.HashMap[String, java.util.List[String]]].asScala.map(x => {
          (x._1, x._2.asScala.map(p => Permissions.getValue(p)).toSet)
        }).toMap
        case None => Map.empty[String, Set[Permissions.Value]]
      }

      User(id = userId, password = password, salt = salt, permissions = permissions)
    }
  }

  /** given id and optionally password and permissions, generate a new user */
  def genUser(id: String, user: UserUpdate, authenticator: AbstractStarChatAuthenticator): Future[User] = Future {

    val password_plain = user.password match {
      case Some(t) => t
      case None =>
        generate_password()
    }

    val salt = user.salt match {
      case Some(t) => t
      case None =>
        generate_salt()
    }

    val password = authenticator.hashedSecret(password = password_plain, salt = salt)

    val permissions = user.permissions match {
      case Some(t) => t
      case None =>
        Map.empty[String, Set[Permissions.Value]]
    }

    User(id = id, password = password, salt = salt, permissions = permissions)
  }

  def generate_password(size: Int = 16): String = {
    RandomNumbers.getString(size)
  }

  def generate_salt(): String = {
    RandomNumbers.getString(16)
  }
}