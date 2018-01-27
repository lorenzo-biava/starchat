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
  val elasticClient = KnowledgeBaseElasticClient
  val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)

  def getIndexName(indexName: String, suffix: Option[String] = None): String = {
    indexName + "." + suffix.getOrElse(elasticClient.kbIndexSuffix)
  }

  def termsSuggester(indexName: String, request: SpellcheckTermsRequest) : Future[Option[SpellcheckTermsResponse]] = Future {
    val client: TransportClient = elasticClient.getClient()

    val suggestionBuilder: TermSuggestionBuilder = new TermSuggestionBuilder("question.base")
    suggestionBuilder.maxEdits(2)
      .prefixLength(request.prefix_length)
      .minDocFreq(request.min_doc_freq)

    val suggestBuilder: SuggestBuilder = new SuggestBuilder()
    suggestBuilder.setGlobalText(request.text)
      .addSuggestion("suggestions", suggestionBuilder)

    val searchBuilder = client.prepareSearch(getIndexName(indexName))
      .setTypes(elasticClient.kbIndexSuffix)
      .suggest(suggestBuilder)

    val searchResponse : SearchResponse = searchBuilder
      .execute()
      .actionGet()

    val suggestions: List[SpellcheckToken] =
      searchResponse.getSuggest.getSuggestion[TermSuggestion]("suggestions")
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
        val spellcheckToken =
          SpellcheckToken(text = text, offset = offset, length = length,
            options = options)
        spellcheckToken
    })

    val response = SpellcheckTermsResponse(tokens = suggestions)
    Option {
      response
    }
  }
}