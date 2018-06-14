package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 01/12/17.
  */

import javax.naming.AuthenticationException

import akka.event.{Logging, LoggingAdapter}
import com.getjenny.analyzer.util.RandomNumbers
import com.getjenny.starchat.SCActorSystem
import com.getjenny.starchat.entities._
import com.getjenny.starchat.services.auth.AbstractStarChatAuthenticator
import com.getjenny.starchat.services.esclient.SystemIndexManagementElasticClient
import com.typesafe.config.{Config, ConfigFactory}
import org.elasticsearch.action.delete.DeleteResponse
import org.elasticsearch.action.get.{GetRequestBuilder, GetResponse}
import org.elasticsearch.action.update.UpdateResponse
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.xcontent.XContentBuilder
import org.elasticsearch.common.xcontent.XContentFactory._
import org.elasticsearch.rest.RestStatus

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.Scalaz._

case class UserEsServiceException(message: String = "", cause: Throwable = None.orNull)
  extends Exception(message, cause)

/**
  * Implements functions, eventually used by IndexManagementResource, for ES index management
  */
class UserEsService extends AbstractUserService {
  private[this] val config: Config = ConfigFactory.load()
  private[this] val elasticClient: SystemIndexManagementElasticClient.type = SystemIndexManagementElasticClient
  private[this] val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)
  private[this] val indexName: String = elasticClient.indexName + "." + elasticClient.userIndexSuffix

  private[this] val admin: String = config.getString("starchat.basic_http_es.admin")
  private[this] val password: String = config.getString("starchat.basic_http_es.password")
  private[this] val salt: String = config.getString("starchat.basic_http_es.salt")
  private[this] val admin_user = User(id = admin, password = password, salt = salt,
    permissions = Map("admin" -> Set(Permissions.admin)))

  def create(user: User): Future[IndexDocumentResult] = Future {

    if(user.id === "admin") {
      throw new AuthenticationException("admin user cannot be changed")
    }

    val builder : XContentBuilder = jsonBuilder().startObject()

    builder.field("id", user.id)
    builder.field("password", user.password)
    builder.field("salt", user.salt)

    val permissions = builder.startObject("permissions")
    user.permissions.foreach{case(permIndexName, userPermissions) =>
      val array = permissions.field(permIndexName).startArray()
      userPermissions.foreach(p => { array.value(p.toString)}) // for each permission
      array.endArray()
    }
    permissions.endObject()

    builder.endObject()

    val client: TransportClient = elasticClient.client
    val response = client.prepareIndex().setIndex(indexName)
      .setCreate(true)
      .setType(elasticClient.userIndexSuffix)
      .setId(user.id)
      .setSource(builder).get()

    val refreshIndex = elasticClient.refresh(indexName)
    if(refreshIndex.failed_shards_n > 0) {
      throw new Exception("User : index refresh failed: (" + indexName + ")")
    }

    val docResult: IndexDocumentResult = IndexDocumentResult(index = response.getIndex,
      dtype = response.getType,
      id = response.getId,
      version = response.getVersion,
      created = response.status === RestStatus.CREATED
    )

    docResult
  }

  def update(id: String, user: UserUpdate):
  Future[UpdateDocumentResult] = Future {

    if(id === "admin") {
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
        user.permissions.getOrElse(Map.empty).foreach{case(permIndexName, userPermissions) =>
          val array = permissions.field(permIndexName).startArray()
          userPermissions.foreach(p => { array.value(p.toString)})
          array.endArray()
        }
        permissions.endObject()
      case None => ;
    }

    builder.endObject()

    val client: TransportClient = elasticClient.client
    val response: UpdateResponse = client.prepareUpdate().setIndex(indexName)
      .setType(elasticClient.userIndexSuffix).setId(id)
      .setDoc(builder)
      .get()

    val refresh_index = elasticClient.refresh(indexName)
    if(refresh_index.failed_shards_n > 0) {
      throw new Exception("User : index refresh failed: (" + indexName + ")")
    }

    val docResult: UpdateDocumentResult = UpdateDocumentResult(index = response.getIndex,
      dtype = response.getType,
      id = response.getId,
      version = response.getVersion,
      created = response.status === RestStatus.CREATED
    )

    docResult
  }

  def delete(id: String): Future[DeleteDocumentResult] = Future {

    if(id === "admin") {
      throw new AuthenticationException("admin user cannot be changed")
    }

    val client: TransportClient = elasticClient.client
    val response: DeleteResponse = client.prepareDelete().setIndex(indexName)
      .setType(elasticClient.userIndexSuffix).setId(id).get()

    val refreshIndex = elasticClient.refresh(indexName)
    if(refreshIndex.failed_shards_n > 0) {
      throw new Exception("User: index refresh failed: (" + indexName + ")")
    }

    val docResult: DeleteDocumentResult = DeleteDocumentResult(index = response.getIndex,
      dtype = response.getType,
      id = response.getId,
      version = response.getVersion,
      found = response.status =/= RestStatus.NOT_FOUND
    )

    docResult
  }

  def read(id: String): Future[User] = Future {
    if(id === "admin") {
      admin_user
    } else {

      val client: TransportClient = elasticClient.client
      val getBuilder: GetRequestBuilder = client.prepareGet(indexName, elasticClient.userIndexSuffix, id)

      val response: GetResponse = getBuilder.get()
      val source = if(response.getSource != None.orNull) {
        response.getSource.asScala.toMap
      } else {
        throw UserEsServiceException("Cannot find user: " + id)
      }

      val userId: String = source.get("id") match {
        case Some(t) => t.asInstanceOf[String]
        case None => throw UserEsServiceException("User id field is empty for: " + id)
      }

      val password: String = source.get("password") match {
        case Some(t) => t.asInstanceOf[String]
        case None => throw UserEsServiceException("Password is empty for the user: " + id)
      }

      val salt: String = source.get("salt") match {
        case Some(t) => t.asInstanceOf[String]
        case None => throw UserEsServiceException("Salt is empty for the user: " + id)
      }

      val permissions: Map[String, Set[Permissions.Value]] = source.get("permissions") match {
        case Some(t) => t.asInstanceOf[java.util.HashMap[String, java.util.List[String]]]
          .asScala.map{case(permIndexName, userPermissions) =>
          (permIndexName, userPermissions.asScala.map(permissionString => Permissions.value(permissionString)).toSet)
        }.toMap
        case None =>
          throw UserEsServiceException("Permissions list is empty for the user: " + id)
      }
      User(id = userId, password = password, salt = salt, permissions = permissions)
    }
  }

  /** given id and optionally password and permissions, generate a new user */
  def genUser(id: String, user: UserUpdate, authenticator: AbstractStarChatAuthenticator): Future[User] = Future {

    val passwordPlain = user.password match {
      case Some(t) => t
      case None =>
        generatePassword()
    }

    val salt = user.salt match {
      case Some(t) => t
      case None =>
        generateSalt()
    }

    val password = authenticator.hashedSecret(password = passwordPlain, salt = salt)

    val permissions = user.permissions match {
      case Some(t) => t
      case None =>
        Map.empty[String, Set[Permissions.Value]]
    }

    User(id = id, password = password, salt = salt, permissions = permissions)
  }

  def generatePassword(size: Int = 16): String = {
    RandomNumbers.getString(size)
  }

  def generateSalt(): String = {
    RandomNumbers.getString(16)
  }
}