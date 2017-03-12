package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 10/03/17.
  */

import com.getjenny.starchat.entities.{IndexManagementResponse, TermGetRequest, _}

import scala.concurrent.{ExecutionContext, Future}
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings._

import scala.io.Source
import java.io._

import org.elasticsearch.action.admin.indices.create.CreateIndexResponse

/**
  * Implements functions, eventually used by IndexManagementResource, for ES index management
  */
class TermService(implicit val executionContext: ExecutionContext) {
  val elastic_client = IndexManagementClient

  def index_term() : Option[IndexDocumentResult] = {
    val client: TransportClient = elastic_client.get_client()

    Option {
      null
    }
  }

  def get_term(terms_request: TermGetRequest) : Option[TermsResults] = {
    val client: TransportClient = elastic_client.get_client()

    Option {
      null
    }
  }

  def update_term() : Option[UpdateDocumentResult] = {
    val client: TransportClient = elastic_client.get_client()

    Option {
      null
    }
  }

  def remove_term() : Option[DeleteDocumentResult] = {
    val client: TransportClient = elastic_client.get_client()

    Option {
      null
    }
  }

  def search_term() : Option[TermsResults] = {
    val client: TransportClient = elastic_client.get_client()

    Option {
      null
    }
  }
}
