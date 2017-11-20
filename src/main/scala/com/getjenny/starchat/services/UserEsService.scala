package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 19/11/17.
  */
import com.getjenny.starchat.entities.{IndexManagementResponse, _}

import scala.concurrent.{ExecutionContext, Future}
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings._

import scala.io.Source
import java.io._

import akka.event.{Logging, LoggingAdapter}
import com.getjenny.starchat.SCActorSystem
import com.getjenny.starchat.routing.auth.UserService
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse
import org.elasticsearch.common.xcontent.XContentType

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Implements functions, eventually used by IndexManagementResource, for ES index management
  */
class UserEsService extends UserService {
  val config: Config = ConfigFactory.load()
  val elastic_client: SystemIndexManagementClient.type = SystemIndexManagementClient
  val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)
  val user_index_name: String = elastic_client.index_name + "." + elastic_client.user_index_suffix

  val admin: String = config.getString("starchat.basic_http_es.admin")
  val password: String = config.getString("starchat.basic_http_es.password")
  val salt: String = config.getString("starchat.basic_http_es.salt")

  //TODO: transform to read/write on ES
  val users = Map("admin" -> User(id = admin, password = password, salt = salt,
    permissions = Map("admin" -> Set(Permissions.admin))),
    "test_user" ->
        User(
          id = "test_user", /** user id */
          password = "3c98bf19cb962ac4cd0227142b3495ab1be46534061919f792254b80c0f3e566f7819cae73bdc616af0ff555f7460ac96d88d56338d659ebd93e2be858ce1cf9", /** user password */
          salt = "salt", /** salt for password hashing */
          permissions = Map("index_0" -> Set(Permissions.read, Permissions.write))
        )
  )

  def create_user(user: User): Future[IndexDocumentResult] = {
    Future {
      IndexDocumentResult(user_index_name, elastic_client.user_index_suffix, user.id, 1, true)
    }
  }

  def update_user(user: User): Future[UpdateDocumentResult] = {
    Future {
      UpdateDocumentResult(user_index_name, elastic_client.user_index_suffix, user.id, 1, true)
    }
  }

  def delete_user(id: String): Future[DeleteDocumentResult] = {
    Future {
      DeleteDocumentResult(user_index_name, elastic_client.user_index_suffix, id, 1, true)
    }
  }

  def get_user(id: String): Future[User] = {
    Future {
      users(id)
    }
  }

  def generate_salt(): Future[String] = {
    val salt = "salt"
    Future {
      salt
    }
  }

}