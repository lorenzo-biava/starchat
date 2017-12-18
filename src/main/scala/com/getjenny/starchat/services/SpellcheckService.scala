package com.getjenny.starchat.services

/**
  * Created by angelo on 21/04/17.
  */

import com.getjenny.starchat.entities._
import scala.concurrent.ExecutionContext
import scala.collection.immutable.List
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.action.search.SearchResponse
import scala.collection.JavaConverters._
import akka.event.{Logging, LoggingAdapter}
import com.getjenny.starchat.SCActorSystem
import akka.http.scaladsl.model.StatusCodes
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

import org.elasticsearch.search.suggest.SuggestBuilder
import org.elasticsearch.search.suggest.term.TermSuggestionBuilder
import org.elasticsearch.search.suggest.term.TermSuggestion
import scala.concurrent.ExecutionContext.Implicits.global

object SpellcheckService {
  val elastic_client = KnowledgeBaseElasticClient
  val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)

  def getIndexName(index_name: String, suffix: Option[String] = None): String = {
    index_name + "." + suffix.getOrElse(elastic_client.kb_index_suffix)
  }

  def termsSuggester(index_name: String, request: SpellcheckTermsRequest) : Future[Option[SpellcheckTermsResponse]] = Future {
    val client: TransportClient = elastic_client.get_client()

    val suggestion_builder: TermSuggestionBuilder = new TermSuggestionBuilder("question.base")
    suggestion_builder.maxEdits(2)
      .prefixLength(request.prefix_length)
      .minDocFreq(request.min_doc_freq)

    val suggest_builder: SuggestBuilder = new SuggestBuilder()
    suggest_builder.setGlobalText(request.text)
      .addSuggestion("suggestions", suggestion_builder)

    val search_builder = client.prepareSearch(getIndexName(index_name))
      .setTypes(elastic_client.kb_index_suffix)
      .suggest(suggest_builder)

    val search_response : SearchResponse = search_builder
      .execute()
      .actionGet()

    val suggestions: List[SpellcheckToken] =
      search_response.getSuggest.getSuggestion[TermSuggestion]("suggestions")
      .getEntries.asScala.toList.map({ case(e) =>
        val item: TermSuggestion.Entry = e
        val text = item.getText.toString
        val offset = item.getOffset
        val length = item.getLength
        val options: List[SpellcheckTokenSuggestions] =
          item.getOptions.asScala.toList.map({ case(e) =>
            val option = SpellcheckTokenSuggestions(
              score = e.getScore.toDouble,
              freq = e.getFreq.toDouble,
              text = e.getText.toString
            )
            option
        })
        val spellcheck_token =
          SpellcheckToken(text = text, offset = offset, length = length,
            options = options)
        spellcheck_token
    })

    val response = SpellcheckTermsResponse(tokens = suggestions)
    Option {
      response
    }
  }
}