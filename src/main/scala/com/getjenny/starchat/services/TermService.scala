package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 10/03/17.
  */

import com.getjenny.starchat.entities.{IndexManagementResponse, TermIdsRequest, _}

import scala.concurrent.{ExecutionContext, Future}
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings._

import scala.io.Source
import java.io._

import org.elasticsearch.action.admin.indices.create.CreateIndexResponse
import org.elasticsearch.action.search.{SearchRequestBuilder, SearchType}
import org.elasticsearch.common.xcontent.XContentBuilder
import org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder

import scala.collection.immutable.Map

/**
  * Implements functions, eventually used by TermResource
  */
class TermService(implicit val executionContext: ExecutionContext) {
  val elastic_client = IndexManagementClient

  def index_term(terms: Terms) : Option[IndexDocumentResult] = {
    val client: TransportClient = elastic_client.get_client()

    Option {
      null
    }
  }

  def get_term(terms_request: TermIdsRequest) : Option[TermsResults] = {
    val client: TransportClient = elastic_client.get_client()

    Option {
      null
    }
  }

  def update_term(terms: Terms) : Option[UpdateDocumentResult] = {
    val client: TransportClient = elastic_client.get_client()

    Option {
      null
    }
  }

  def remove_term(termGetRequest: TermIdsRequest) : Option[DeleteDocumentResult] = {
    val client: TransportClient = elastic_client.get_client()

    Option {
      null
    }
  }

  def search_term(term: Term) : Option[TermsResults] = {
    val client: TransportClient = elastic_client.get_client()

    Option {
      null
    }
  }
}
