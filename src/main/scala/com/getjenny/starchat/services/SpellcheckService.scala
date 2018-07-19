package com.getjenny.starchat.services

/**
  * Created by angelo on 21/04/17.
  */

import akka.event.{Logging, LoggingAdapter}
import com.getjenny.starchat.SCActorSystem
import com.getjenny.starchat.entities._
import com.getjenny.starchat.services.esclient.KnowledgeBaseElasticClient
import com.getjenny.starchat.utils.Index
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.search.suggest.SuggestBuilder
import org.elasticsearch.search.suggest.term.{TermSuggestion, TermSuggestionBuilder}

import scala.collection.JavaConverters._
import scala.collection.immutable.List
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object SpellcheckService {
  private[this] val elasticClient = KnowledgeBaseElasticClient
  private[this] val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)

  def termsSuggester(indexName: String, request: SpellcheckTermsRequest) : Future[Option[SpellcheckTermsResponse]] = Future {
    val client: TransportClient = elasticClient.client

    val suggestionBuilder: TermSuggestionBuilder = new TermSuggestionBuilder("question.base")
    suggestionBuilder.maxEdits(request.max_edit)
      .prefixLength(request.prefix_length)
      .minDocFreq(request.min_doc_freq)

    val suggestBuilder: SuggestBuilder = new SuggestBuilder()
    suggestBuilder.setGlobalText(request.text)
      .addSuggestion("suggestions", suggestionBuilder)

    val searchBuilder = client.prepareSearch(Index.indexName(indexName, elasticClient.indexSuffix))
      .setTypes(elasticClient.indexSuffix)
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
