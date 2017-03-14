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
import org.elasticsearch.common.xcontent.XContentFactory._
import org.elasticsearch.action.bulk._
import org.elasticsearch.rest.RestStatus

import scala.collection.immutable.Map

/**
  * Implements functions, eventually used by TermResource
  */
class TermService(implicit val executionContext: ExecutionContext) {
  val elastic_client = IndexManagementClient

  def vector2string(vector: Vector[Double]): String = {
    vector.zipWithIndex.map(x => (x._2.toString + "|" + x._1.toString)).mkString(" ")
  }

  def index_term(terms: Terms) : Option[List[IndexDocumentResult]] = {
    val client: TransportClient = elastic_client.get_client()

    val bulkRequest : BulkRequestBuilder = client.prepareBulk()

    terms.terms.foreach( term => {
      val builder : XContentBuilder = jsonBuilder().startObject()
      builder.field("term", term.term)
      term.synonyms match {
        case Some(t) => builder.field("synonyms", t)
        case None => ;
      }
      term.antonyms match {
        case Some(t) => builder.field("antonyms", t)
        case None => ;
      }
      term.frequency match {
        case Some(t) => builder.field("frequency", t)
        case None => ;
      }
      val indexable_vector: String = vector2string(term.vector)
      builder.field("vector", indexable_vector)
      builder.endObject()

      bulkRequest.add(client.prepareIndex(elastic_client.index_name, elastic_client.term_type_name)
        .setSource(builder))
    })

    val bulkResponse: BulkResponse = bulkRequest.get()

    val results: List[IndexDocumentResult] = bulkResponse.getItems.map(x => {
      IndexDocumentResult(x.getIndex, x.getType, x.getId,
      x.getVersion,
      x.status == RestStatus.CREATED)
    }).toList
    
    Option {
      results
    }
  }

  def get_term(terms_request: TermIdsRequest) : Option[TermsResults] = {
    val client: TransportClient = elastic_client.get_client()

    Option {
      null
    }
  }

  def update_term(terms: Terms) : Option[List[UpdateDocumentResult]] = {
    val client: TransportClient = elastic_client.get_client()

    val bulkRequest : BulkRequestBuilder = client.prepareBulk()

    terms.terms.foreach( term => {
      val builder : XContentBuilder = jsonBuilder().startObject()
      builder.field("term", term.term)
      term.synonyms match {
        case Some(t) => builder.field("synonyms", t)
        case None => ;
      }
      term.antonyms match {
        case Some(t) => builder.field("antonyms", t)
        case None => ;
      }
      term.frequency match {
        case Some(t) => builder.field("frequency", t)
        case None => ;
      }
      val indexable_vector: String = vector2string(term.vector)
      builder.field("vector", indexable_vector)
      builder.endObject()

      bulkRequest.add(client.prepareIndex(elastic_client.index_name, elastic_client.term_type_name)
        .setSource(builder)
      )
    })

    val bulkResponse: BulkResponse = bulkRequest.get()

    val results: List[UpdateDocumentResult] = bulkResponse.getItems.map(x => {
      UpdateDocumentResult(x.getIndex, x.getType, x.getId,
        x.getVersion,
        x.status == RestStatus.CREATED)
    }).toList

    Option {
      results
    }
  }

  def remove_term(termGetRequest: TermIdsRequest) : Option[List[DeleteDocumentResult]] = {
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
