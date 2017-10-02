package com.getjenny.starchat.analyzer.atoms

/**
  * Created by mal on 20/02/2017.
  */

import com.getjenny.analyzer.atoms.AbstractAtomic
import com.getjenny.analyzer.expressions.{Data, Result}
import com.getjenny.starchat.entities._

import scala.concurrent.{Await, ExecutionContext, Future}
import com.getjenny.starchat.services._
import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.action.search.{SearchRequestBuilder, SearchResponse, SearchType}
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.index.query.{BoolQueryBuilder, InnerHitBuilder, QueryBuilder, QueryBuilders}
import org.elasticsearch.search.SearchHit

import scala.concurrent.duration._
import scala.concurrent._
import ExecutionContext.Implicits.global
import scala.collection.immutable.{List, Map}

import scala.collection.JavaConverters._

/**
  * Query ElasticSearch
  */
class SearchAtomic(arguments: List[String]) extends AbstractAtomic {
  val state = arguments(0)
  override def toString: String = "search(\"" + state + "\")"
  val isEvaluateNormalized: Boolean = false
  val ref_state: String = state

  override val match_threshold: Double = 0.65

  val elastic_client = DecisionTableElasticClient
  val min_score = Option{elastic_client.query_min_threshold}
  val boost_exact_match_factor = Option{elastic_client.boost_exact_match_factor}
  val queries_score_mode = Map[String, ScoreMode]("min" -> ScoreMode.Min, "max" -> ScoreMode.Max,
    "avg" -> ScoreMode.Avg, "total" -> ScoreMode.Total)

  def search(documentSearch: DTDocumentSearch): Future[Option[(Int, Float, List[Float])]] = {
    val client: TransportClient = elastic_client.get_client()
    val search_builder : SearchRequestBuilder = client.prepareSearch(elastic_client.index_name)
      .setTypes(elastic_client.type_name)
      .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)

    val min_score = documentSearch.min_score.getOrElse(
      Option{elastic_client.query_min_threshold}.getOrElse(0.0f)
    )

    val boost_exact_match_factor = documentSearch.boost_exact_match_factor.getOrElse(
      Option{elastic_client.boost_exact_match_factor}.getOrElse(1.0f)
    )

    search_builder.setMinScore(min_score)

    val bool_query_builder : BoolQueryBuilder = QueryBuilders.boolQuery()
    if (documentSearch.state.isDefined)
      bool_query_builder.must(QueryBuilders.termQuery("state", documentSearch.state.get))

    if(documentSearch.queries.isDefined) {
      val nested_query: QueryBuilder = QueryBuilders.nestedQuery(
        "queries",
        QueryBuilders.boolQuery()
          .must(QueryBuilders.matchQuery("queries.query.stem_bm25", documentSearch.queries.get))
          .should(QueryBuilders.matchPhraseQuery("queries.query.raw", documentSearch.queries.get)
            .boost(1 + (min_score * boost_exact_match_factor))
          ),
        queries_score_mode.getOrElse(elastic_client.queries_score_mode, ScoreMode.Max)
      ).ignoreUnmapped(true).innerHit(new InnerHitBuilder().setSize(10000))
      bool_query_builder.must(nested_query)
    }

    search_builder.setQuery(bool_query_builder)

    val search_response : SearchResponse = search_builder
      .setFrom(documentSearch.from.getOrElse(0)).setSize(documentSearch.size.getOrElse(10))
      .execute()
      .actionGet()

    val documents : Option[List[Float]] =
      Option {
        search_response.getHits.getHits.toList.map({
          case(e) =>
            val item: SearchHit = e
            item.getScore
        })
      }

    val filtered_doc : List[Float] = documents.getOrElse(List[Float]())

    val max_score : Float = search_response.getHits.getMaxScore
    val total : Int = filtered_doc.length
    val search_results = (total, max_score, filtered_doc)

    val search_results_option : Future[Option[(Int, Float, List[Float])]] = Future { Option { search_results } }
    search_results_option
  }

  def evaluate(query: String, data: Data = Data()): Result = {
    val dtDocumentSearch : DTDocumentSearch =
      DTDocumentSearch(from = Option{0}, size = Option{10}, min_score = min_score,
        execution_order = None: Option[Int],
        boost_exact_match_factor = boost_exact_match_factor, state = Option{ref_state}, queries = Option{query})

    val state: Future[Option[(Int, Float, List[Float])]] = this.search(dtDocumentSearch)
    //search the state with the closest query value, then return that state
    val res : Option[(Int, Float, List[Float])] = Await.result(state, 30.seconds)

    val res_count = if (res.isEmpty) 0 else res.get._1

    val score : Double = res_count match {
      case 0 => 0.0f
      case _ =>
        val max_score : Double = res.get._2
        val sum_of_scores : Double = res.get._3.sum
        val norm_score = max_score / (sum_of_scores + 1)
        norm_score
    }
    Result(score=score)
  } // returns elasticsearch score of the highest query in queries

}
