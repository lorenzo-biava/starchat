package com.getjenny.starchat.services

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 01/07/16.
  */

import akka.event.{Logging, LoggingAdapter}
import com.getjenny.analyzer.util.{RandomNumbers, Time}
import com.getjenny.starchat.SCActorSystem
import com.getjenny.starchat.entities.{Doctypes, _}
import com.getjenny.starchat.services.esclient.QuestionAnswerElasticClient
import com.getjenny.starchat.utils.Index
import org.apache.lucene.search.join._
import org.elasticsearch.action.get.{GetResponse, MultiGetItemResponse, MultiGetRequest, MultiGetResponse}
import org.elasticsearch.action.index.{IndexRequest, IndexResponse}
import org.elasticsearch.action.search.{SearchRequest, SearchResponse, SearchType}
import org.elasticsearch.action.update.{UpdateRequest, UpdateResponse}
import org.elasticsearch.client.{RequestOptions, RestHighLevelClient}
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.action.bulk.BulkRequestBuilder
import org.elasticsearch.common.xcontent.XContentBuilder
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.common.xcontent.XContentFactory._
import org.elasticsearch.index.query.functionscore._
import org.elasticsearch.index.query.{BoolQueryBuilder, InnerHitBuilder, QueryBuilder, QueryBuilders}
import org.elasticsearch.rest.RestStatus
import org.elasticsearch.script._
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.metrics.cardinality.Cardinality
import org.elasticsearch.search.aggregations.metrics.sum.Sum
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.sort.{FieldSortBuilder, ScoreSortBuilder, SortOrder}
import scalaz.Scalaz._
import org.elasticsearch.action.bulk.BulkResponse
import org.elasticsearch.client.RequestOptions

import scala.collection.JavaConverters._
import scala.collection.immutable.{List, Map}
import scala.collection.mutable
import scala.concurrent.Future

case class QuestionAnswerServiceException(message: String = "", cause: Throwable = None.orNull)
  extends Exception(message, cause)

trait QuestionAnswerService extends AbstractDataService {
  override val elasticClient: QuestionAnswerElasticClient

  val manausTermsExtractionService: ManausTermsExtractionService.type = ManausTermsExtractionService
  val log: LoggingAdapter = Logging(SCActorSystem.system, this.getClass.getCanonicalName)

  val nested_score_mode: Map[String, ScoreMode] = Map[String, ScoreMode]("min" -> ScoreMode.Min,
    "max" -> ScoreMode.Max, "avg" -> ScoreMode.Avg, "total" -> ScoreMode.Total)

  var cacheStealTimeMillis: Int

  private[this] def calcDictSize(indexName: String): DictSize = {
    val client: RestHighLevelClient = elasticClient.httpClient

    val questionAgg = AggregationBuilders.cardinality("question_term_count").field("question.base")
    val answerAgg = AggregationBuilders.cardinality("answer_term_count").field("answer.base")

    val scriptBody = "def qnList = new ArrayList(doc[\"question.base\"].getValues()) ; " +
      "List anList = doc[\"answer.base\"].getValues() ; qnList.addAll(anList) ; return qnList ;"
    val script: Script = new Script(scriptBody)
    val totalAgg = AggregationBuilders.cardinality("total_term_count").script(script)

    val sourceReq: SearchSourceBuilder = new SearchSourceBuilder()
      .query(QueryBuilders.matchAllQuery)
      .size(0)
      .aggregation(questionAgg)
      .aggregation(answerAgg)
      .aggregation(totalAgg)


    val searchReq = new SearchRequest(Index.indexName(indexName, elasticClient.indexSuffix))
      .source(sourceReq)
      .types(elasticClient.indexMapping)
      .searchType(SearchType.DFS_QUERY_THEN_FETCH)
      .requestCache(true)

    val searchResp: SearchResponse = client.search(searchReq, RequestOptions.DEFAULT)

    val totalHits = searchResp.getHits.totalHits

    val questionAggRes: Cardinality = searchResp.getAggregations.get("question_term_count")
    val answerAggRes: Cardinality = searchResp.getAggregations.get("answer_term_count")
    val totalAggRes: Cardinality = searchResp.getAggregations.get("total_term_count")

    DictSize(numDocs = totalHits,
      question = questionAggRes.getValue,
      answer = answerAggRes.getValue,
      total = totalAggRes.getValue
    )
  }

  var dictSizeCacheMaxSize: Int
  private[this] val dictSizeCache: mutable.LinkedHashMap[String, (Long, DictSize)] =
    mutable.LinkedHashMap[String, (Long, DictSize)]()
  def dictSize(indexName: String, stale: Long = cacheStealTimeMillis): DictSize = {
    val key = indexName
    dictSizeCache.get(key) match {
      case Some((lastUpdateTs,dictSize)) =>
        val cacheStaleTime = math.abs(Time.timestampMillis - lastUpdateTs)
        if(cacheStaleTime < stale) {
          dictSize
        } else {
          val result = calcDictSize(indexName = indexName)
          if (dictSizeCache.size >= dictSizeCacheMaxSize) {
            dictSizeCache.head match { case (oldestTerm, (_, _)) => dictSizeCache -= oldestTerm}
          }
          dictSizeCache.remove(key)
          dictSizeCache.update(key, (Time.timestampMillis, result))
          result
        }
      case _ =>
        val result = calcDictSize(indexName = indexName)
        if (dictSizeCache.size >= dictSizeCacheMaxSize) {
          dictSizeCache.head match { case (oldestTerm, (_, _)) => dictSizeCache -= oldestTerm}
        }
        dictSizeCache.update(key, (Time.timestampMillis, result))
        result
    }
  }

  def dictSizeFuture(indexName: String, stale: Long = cacheStealTimeMillis): Future[DictSize] = Future {
    dictSize(indexName = indexName, stale = stale)
  }

  private[this] def calcTotalTerms(indexName: String): TotalTerms = {
    val client: RestHighLevelClient = elasticClient.httpClient

    val questionAgg = AggregationBuilders.sum("question_term_count").field("question.base_length")
    val answerAgg = AggregationBuilders.sum("answer_term_count").field("answer.base_length")

    val sourceReq: SearchSourceBuilder = new SearchSourceBuilder()
      .query(QueryBuilders.matchAllQuery)
      .size(0)
      .aggregation(questionAgg)
      .aggregation(answerAgg)

    val searchReq = new SearchRequest(Index.indexName(indexName, elasticClient.indexSuffix))
      .source(sourceReq)
      .types(elasticClient.indexMapping)
      .searchType(SearchType.DFS_QUERY_THEN_FETCH)
      .requestCache(true)

    val searchResp: SearchResponse = client.search(searchReq, RequestOptions.DEFAULT)

    val totalHits = searchResp.getHits.totalHits

    val questionAggRes: Sum = searchResp.getAggregations.get("question_term_count")
    val answerAggRes: Sum = searchResp.getAggregations.get("answer_term_count")

    TotalTerms(numDocs = totalHits,
      question = questionAggRes.getValue.toLong,
      answer = answerAggRes.getValue.toLong)
  }

  var totalTermsCacheMaxSize: Int
  private[this] val totalTermsCache: mutable.LinkedHashMap[String, (Long, TotalTerms)] =
    mutable.LinkedHashMap[String, (Long, TotalTerms)]()
  def totalTerms(indexName: String, stale: Long = cacheStealTimeMillis): TotalTerms = {
    val key = indexName
    totalTermsCache.get(key) match {
      case Some((lastUpdateTs,dictSize)) =>
        val cacheStaleTime = math.abs(Time.timestampMillis - lastUpdateTs)
        if(cacheStaleTime < stale) {
          dictSize
        } else {
          val result = calcTotalTerms(indexName = indexName)
          if (totalTermsCache.size >= totalTermsCacheMaxSize) {
            totalTermsCache.head match { case (oldestTerm, (_, _)) => totalTermsCache -= oldestTerm}
          }
          totalTermsCache.remove(key)
          totalTermsCache.update(key, (Time.timestampMillis, result))
          result
        }
      case _ =>
        val result = calcTotalTerms(indexName = indexName)
        if (totalTermsCache.size >= totalTermsCacheMaxSize) {
          totalTermsCache.head match { case (oldestTerm, (_, _)) => totalTermsCache -= oldestTerm}
        }
        totalTermsCache.update(key, (Time.timestampMillis, result))
        result
    }
  }

  def totalTermsFuture(indexName: String, stale: Long = cacheStealTimeMillis): Future[TotalTerms] = Future {
    totalTerms(indexName = indexName, stale = stale)
  }

  def calcTermCount(indexName: String,
                    field: TermCountFields.Value = TermCountFields.question, term: String): TermCount = {
    val client: RestHighLevelClient = elasticClient.httpClient

    val script: Script = new Script("_score")

    val agg = AggregationBuilders.sum("countTerms").script(script)

    val esFieldName: String = field match {
      case TermCountFields.question => "question.freq"
      case TermCountFields.answer => "answer.freq"
    }

    val boolQueryBuilder : BoolQueryBuilder = QueryBuilders.boolQuery()
      .must(QueryBuilders.matchQuery(esFieldName, term))

    val sourceReq: SearchSourceBuilder = new SearchSourceBuilder()
      .query(boolQueryBuilder)
      .size(0)
      .aggregation(agg)

    val searchReq = new SearchRequest(Index.indexName(indexName, elasticClient.indexSuffix))
      .source(sourceReq)
      .types(elasticClient.indexMapping)
      .searchType(SearchType.DFS_QUERY_THEN_FETCH)
      .requestCache(true)

    val searchResp: SearchResponse = client.search(searchReq, RequestOptions.DEFAULT)

    val totalHits = searchResp.getHits.totalHits

    val aggRes: Sum = searchResp.getAggregations.get("countTerms")

    TermCount(numDocs = totalHits,
      count = aggRes.getValue.toLong)
  }

  var countTermCacheMaxSize: Int
  private[this] val countTermCache: mutable.LinkedHashMap[String, (Long, TermCount)] =
    mutable.LinkedHashMap[String, (Long, TermCount)]()
  def termCount(indexName: String, field: TermCountFields.Value, term: String,
                stale: Long = cacheStealTimeMillis): TermCount = {
    val key = indexName + field + term
    countTermCache.get(key) match {
      case Some((lastUpdateTs,dictSize)) =>
        val cacheStaleTime = math.abs(Time.timestampMillis - lastUpdateTs)
        if(cacheStaleTime < stale) {
          dictSize
        } else {
          val result = calcTermCount(indexName = indexName, field = field, term = term)
          if (countTermCache.size > countTermCacheMaxSize) {
            countTermCache.head match { case (oldestTerm, (_, _)) => countTermCache -= oldestTerm}
          }
          countTermCache.remove(key)
          countTermCache.update(key, (Time.timestampMillis, result))
          result
        }
      case _ =>
        val result = calcTermCount(indexName = indexName, field = field, term = term)
        if (countTermCache.size > countTermCacheMaxSize) {
          countTermCache.head match { case (oldestTerm, (_, _)) => countTermCache -= oldestTerm}
        }
        countTermCache.update(key, (Time.timestampMillis, result))
        result
    }
  }

  def termCountFuture(indexName: String, field: TermCountFields.Value, term: String,
                      stale: Long = cacheStealTimeMillis): Future[TermCount] = Future {
    termCount(indexName, field, term, stale)
  }

  def countersCacheParameters(parameters: CountersCacheParameters): CountersCacheParameters = {
    parameters.dictSizeCacheMaxSize match {
      case Some(v) => this.dictSizeCacheMaxSize = v
      case _ => ;
    }

    parameters.totalTermsCacheMaxSize match {
      case Some(v) => this.totalTermsCacheMaxSize = v
      case _ => ;
    }

    parameters.countTermCacheMaxSize match {
      case Some(v) => this.countTermCacheMaxSize = v
      case _ => ;
    }

    parameters.cacheStealTimeMillis match {
      case Some(v) => this.cacheStealTimeMillis = v
      case _ => ;
    }

    CountersCacheParameters(
      dictSizeCacheMaxSize = Some(dictSizeCacheMaxSize),
      totalTermsCacheMaxSize = Some(totalTermsCacheMaxSize),
      countTermCacheMaxSize = Some(countTermCacheMaxSize),
      cacheStealTimeMillis = Some(cacheStealTimeMillis)
    )
  }

  def countersCacheParameters: (CountersCacheParameters, CountersCacheSize) = {
    (CountersCacheParameters(
      dictSizeCacheMaxSize = Some(dictSizeCacheMaxSize),
      totalTermsCacheMaxSize = Some(totalTermsCacheMaxSize),
      countTermCacheMaxSize = Some(countTermCacheMaxSize),
      cacheStealTimeMillis = Some(cacheStealTimeMillis)
    ),
      CountersCacheSize(
        dictSizeCacheSize = dictSizeCache.size,
        totalTermsCacheSize = totalTermsCache.size,
        countTermCacheSize = countTermCache.size
      ))
  }


  def countersCacheReset: (CountersCacheParameters, CountersCacheSize) = {
    dictSizeCache.clear()
    countTermCache.clear()
    totalTermsCache.clear()
    countersCacheParameters
  }

  def search(indexName: String, documentSearch: QADocumentSearch): Future[Option[SearchQADocumentsResults]] = {
    val client: RestHighLevelClient = elasticClient.httpClient

    val sourceReq: SearchSourceBuilder = new SearchSourceBuilder()
      .from(documentSearch.from.getOrElse(0))
      .size(documentSearch.size.getOrElse(10))
      .minScore(documentSearch.minScore.getOrElse(Option{elasticClient.queryMinThreshold}.getOrElse(0.0f)))

    documentSearch.sortByConvIdIdx match {
      case Some(true) =>
        sourceReq.sort(new FieldSortBuilder("conversation").order(SortOrder.DESC))
          .sort(new FieldSortBuilder("index_in_conversation").order(SortOrder.DESC))
          .sort(new FieldSortBuilder("timestamp").order(SortOrder.DESC))
      case _ => sourceReq.sort(new ScoreSortBuilder().order(SortOrder.DESC))
    }

    val searchReq = new SearchRequest(Index.indexName(indexName, elasticClient.indexSuffix))
      .source(sourceReq)
      .types(elasticClient.indexMapping)
      .searchType(SearchType.DFS_QUERY_THEN_FETCH)

    val boolQueryBuilder : BoolQueryBuilder = QueryBuilders.boolQuery()

    documentSearch.conversation match {
      case Some(value) =>
        boolQueryBuilder.must(QueryBuilders.matchQuery("conversation", value))
      case _ => ;
    }

    documentSearch.indexInConversation match {
      case Some(value) =>
        boolQueryBuilder.must(QueryBuilders.matchQuery("index_in_conversation", value))
      case _ => ;
    }

    documentSearch.status match {
      case Some(status) => boolQueryBuilder.filter(QueryBuilders.termQuery("status", status))
      case _ => ;
    }

    documentSearch.timestampGte match {
      case Some(ts) =>
        boolQueryBuilder.filter(
          QueryBuilders.rangeQuery("timestamp")
            .gte(ts))
      case _ => ;
    }

    documentSearch.timestampLte match {
      case Some(ts) =>
        boolQueryBuilder.filter(
          QueryBuilders.rangeQuery("timestamp")
            .lte(ts))
      case _ => ;
    }

    documentSearch.random.filter(identity) match {
      case Some(true) =>
        val randomBuilder = new RandomScoreFunctionBuilder().seed(RandomNumbers.integer)
        val functionScoreQuery: QueryBuilder = QueryBuilders.functionScoreQuery(randomBuilder)
        boolQueryBuilder.must(functionScoreQuery)
      case _ => ;
    }

    val coreDataIn = documentSearch.coreData.getOrElse(QADocumentCoreUpdate())
    val annotationsIn = documentSearch.annotations.getOrElse(QADocumentAnnotationsUpdate())

    // begin core data
    coreDataIn.question match {
      case Some(questionQuery) =>
        boolQueryBuilder.must(QueryBuilders.boolQuery()
          .must(QueryBuilders.matchQuery("question.stem", questionQuery))
          .should(QueryBuilders.matchPhraseQuery("question.raw", questionQuery)
            .boost(elasticClient.questionExactMatchBoost))
        )

        val questionNegativeNestedQuery: QueryBuilder = QueryBuilders.nestedQuery(
          "question_negative",
          QueryBuilders.matchQuery("question_negative.query.base", questionQuery)
            .minimumShouldMatch(elasticClient.questionNegativeMinimumMatch)
            .boost(elasticClient.questionNegativeBoost),
          ScoreMode.Total
        ).ignoreUnmapped(true)
          .innerHit(new InnerHitBuilder().setSize(100))

        boolQueryBuilder.should(
          questionNegativeNestedQuery
        )
      case _ => ;
    }

    coreDataIn.questionScoredTerms match {
      case Some(questionScoredTerms) =>
        val queryTerms = QueryBuilders.boolQuery()
          .should(QueryBuilders.matchQuery("question_scored_terms.term", questionScoredTerms))
        val script: Script = new Script("doc[\"question_scored_terms.score\"].value")
        val scriptFunction = new ScriptScoreFunctionBuilder(script)
        val functionScoreQuery: QueryBuilder = QueryBuilders.functionScoreQuery(queryTerms, scriptFunction)

        val nestedQuery: QueryBuilder = QueryBuilders.nestedQuery(
          "question_scored_terms",
          functionScoreQuery,
          nested_score_mode.getOrElse(elasticClient.queriesScoreMode, ScoreMode.Total)
        ).ignoreUnmapped(true).innerHit(new InnerHitBuilder().setSize(100))
        boolQueryBuilder.should(nestedQuery)
      case _ => ;
    }

    coreDataIn.answer match {
      case Some(value) =>
        boolQueryBuilder.must(QueryBuilders.matchQuery("answer.stem", value))
      case _ => ;
    }

    coreDataIn.answerScoredTerms match {
      case Some(answerScoredTerms) =>
        val queryTerms = QueryBuilders.boolQuery()
          .should(QueryBuilders.matchQuery("answer_scored_terms.term", answerScoredTerms))
        val script: Script = new Script("doc[\"answer_scored_terms.score\"].value")
        val scriptFunction = new ScriptScoreFunctionBuilder(script)
        val functionScoreQuery: QueryBuilder = QueryBuilders.functionScoreQuery(queryTerms, scriptFunction)

        val nestedQuery: QueryBuilder = QueryBuilders.nestedQuery(
          "answer_scored_terms",
          functionScoreQuery,
          nested_score_mode.getOrElse(elasticClient.queriesScoreMode, ScoreMode.Total)
        ).ignoreUnmapped(true).innerHit(new InnerHitBuilder().setSize(100))
        boolQueryBuilder.should(nestedQuery)
      case _ => ;
    }

    coreDataIn.topics match {
      case Some(topics) => boolQueryBuilder.filter(QueryBuilders.termQuery("topics.base", topics))
      case _ => ;
    }

    coreDataIn.verified match {
      case Some(verified) => boolQueryBuilder.filter(QueryBuilders.termQuery("verified", verified))
      case _ => ;
    }

    coreDataIn.done match {
      case Some(done) => boolQueryBuilder.filter(QueryBuilders.termQuery("done", done))
      case _ => ;
    }
    // end core data

    // begin annotations
    annotationsIn.dclass match {
      case Some(dclass) => boolQueryBuilder.filter(QueryBuilders.termQuery("dclass", dclass))
      case _ => ;
    }

    annotationsIn.doctype match {
      case Some(doctype) => boolQueryBuilder.filter(QueryBuilders.termQuery("doctype", doctype))
      case _ => ;
    }

    annotationsIn.state match {
      case Some(state) => boolQueryBuilder.filter(QueryBuilders.termQuery("state", state))
      case _ => ;
    }

    annotationsIn.agent match {
      case Some(agent) => boolQueryBuilder.filter(QueryBuilders.termQuery("agent", agent))
      case _ => ;
    }

    annotationsIn.escalated match {
      case Some(escalated) => boolQueryBuilder.filter(QueryBuilders.termQuery("escalated", escalated))
      case _ => ;
    }

    annotationsIn.answered match {
      case Some(answered) => boolQueryBuilder.filter(QueryBuilders.termQuery("answered", answered))
      case _ => ;
    }

    annotationsIn.triggered match {
      case Some(triggered) => boolQueryBuilder.filter(QueryBuilders.termQuery("triggered", triggered))
      case _ => ;
    }

    annotationsIn.followup match {
      case Some(followup) => boolQueryBuilder.filter(QueryBuilders.termQuery("followup", followup))
      case _ => ;
    }

    annotationsIn.feedbackConv {
      case Some(feedbackConv) => boolQueryBuilder.filter(QueryBuilders.termQuery("feedbackConv", feedbackConv))
      case _ => ;
    }

    annotationsIn.feedbackConvScore {
      case Some(feedbackConvScore) => boolQueryBuilder.filter(
        QueryBuilders.termQuery("feedbackConvScore", feedbackConvScore))
      case _ => ;
    }

    annotationsIn.algorithmConvScore {
      case Some(algorithmConvScore) => boolQueryBuilder.filter(
        QueryBuilders.termQuery("algorithmConvScore", algorithmConvScore))
      case _ => ;
    }

    annotationsIn.feedbackAnswerScore {
      case Some(feedbackAnswerScore) => boolQueryBuilder.filter(
        QueryBuilders.termQuery("feedbackAnswerScore", feedbackAnswerScore))
      case _ => ;
    }

    annotationsIn.algorithmAnswerScore {
      case Some(algorithmAnswerScore) => boolQueryBuilder.filter(
        QueryBuilders.termQuery("algorithmAnswerScore", algorithmAnswerScore))
      case _ => ;
    }

    annotationsIn.start {
      case Some(start) => boolQueryBuilder.filter(QueryBuilders.termQuery("start", start))
      case _ => ;
    }
    // end annotations

    sourceReq.query(boolQueryBuilder)

    val searchResp: SearchResponse = client.search(searchReq, RequestOptions.DEFAULT)

    val documents : Option[List[SearchQADocument]] = Option {
      searchResp.getHits.getHits.toList.map { item =>
        // val fields : Map[String, GetField] = item.getFields.toMap
        val id : String = item.getId

        // val score : Float = fields.get("_score").asInstanceOf[Float]
        val source : Map[String, Any] = item.getSourceAsMap.asScala.toMap

        val conversation : String = source.get("conversation") match {
          case Some(t) => t.asInstanceOf[String]
          case _ => throw QuestionAnswerServiceException("Missing conversation ID for " +
            "index:docId(" + indexName + ":"  + id + ")")
        }

        val indexInConversation : Int = source.get("index_in_conversation") match {
          case Some(t) => t.asInstanceOf[Int]
          case _ => throw QuestionAnswerServiceException("Missing index in conversation for " +
            "index:docId(" + indexName + ":"  + id + ")")
        }

        val status : Int = source.get("status") match {
          case Some(t) => t.asInstanceOf[Int]
          case _ => 0
        }

        val timestamp : Option[Long] = source.get("timestamp") match {
          case Some(t) => Option { t.asInstanceOf[Long] }
          case _ => None : Option[Long]
        }

        // begin core data
        val question : Option[String] = source.get("question") match {
          case Some(t) => Some(t.asInstanceOf[String])
          case _ => None
        }

        val questionNegative : Option[List[String]] = source.get("question_negative") match {
          case Some(t) =>
            val res = t.asInstanceOf[java.util.ArrayList[java.util.HashMap[String, String]]]
              .asScala.map(_.asScala.get("query")).filter(_.nonEmpty).map(_.get).toList
            Option { res }
          case _ => None: Option[List[String]]
        }

        val questionScoredTerms: Option[List[(String, Double)]] = source.get("question_scored_terms") match {
          case Some(t) => Option {
            t.asInstanceOf[java.util.ArrayList[java.util.HashMap[String, Any]]].asScala
              .map(pair =>
                (pair.getOrDefault("term", "").asInstanceOf[String],
                  pair.getOrDefault("score", 0.0).asInstanceOf[Double]))
              .toList
          }
          case _ => None : Option[List[(String, Double)]]
        }

        val answer : Option[String] = source.get("answer") match {
          case Some(t) => Some(t.asInstanceOf[String])
          case _ => None
        }

        val answerScoredTerms: Option[List[(String, Double)]] = source.get("answer_scored_terms") match {
          case Some(t) => Option {
            t.asInstanceOf[java.util.ArrayList[java.util.HashMap[String, Any]]].asScala
              .map(pair =>
                (pair.getOrDefault("term", "").asInstanceOf[String],
                  pair.getOrDefault("score", 0.0).asInstanceOf[Double]))
              .toList
          }
          case _ => None : Option[List[(String, Double)]]
        }

        val topics : Option[String] = source.get("topics") match {
          case Some(t) => Option { t.asInstanceOf[String] }
          case _ => None : Option[String]
        }

        val verified : Option[Boolean] = source.get("verified") match {
          case Some(t) => Some(t.asInstanceOf[Boolean])
          case _ => Some(false)
        }

        val done : Option[Boolean] = source.get("done") match {
          case Some(t) => Some(t.asInstanceOf[Boolean])
          case _ => Some(false)
        }

        val coreDataOut: Option[QADocumentCore] = Some {
          QADocumentCore(
            question = question,
            questionNegative = questionNegative,
            questionScoredTerms = questionScoredTerms,
            answer = answer,
            answerScoredTerms = answerScoredTerms,
            topics = topics,
            verified = verified,
            done = done
          )
        }
        // begin core data

        // begin annotations
        val dclass : Option[String] = source.get("dclass") match {
          case Some(t) => Option { t.asInstanceOf[String] }
          case _ => None : Option[String]
        }

        val doctype : Doctypes.Value = source.get("doctype") match {
          case Some(t) => Doctypes.value(t.asInstanceOf[String])
          case _ => Doctypes.NORMAL
        }

        val state : Option[String] = source.get("state") match {
          case Some(t) => Option { t.asInstanceOf[String] }
          case _ => None : Option[String]
        }

        val agent : Agent.Value = source.get("agent") match {
          case Some(t) => Agent.value(t.asInstanceOf[String])
          case _ => Agent.UNSPECIFIED
        }

        val escalated : Escalated.Value = source.get("escalated") match {
          case Some(t) => Escalated.value(t.asInstanceOf[String])
          case _ => Escalated.UNSPECIFIED
        }

        val answered : Answered.Value = source.get("answered") match {
          case Some(t) => Answered.value(t.asInstanceOf[String])
          case _ => Answered.UNSPECIFIED
        }

        val triggered : Triggered.Value = source.get("triggered") match {
          case Some(t) => Triggered.value(t.asInstanceOf[String])
          case _ => Triggered.UNSPECIFIED
        }

        val followup : Followup.Value = source.get("followup") match {
          case Some(t) => Followup.value(t.asInstanceOf[String])
          case _ => Followup.UNSPECIFIED
        }

        val feedbackConv : Option[String] = source.get("feedbackConv") match {
          case Some(t) => Option { t.asInstanceOf[String] }
          case _ => None : Option[String]
        }

        val feedbackConvScore : Option[Double] = source.get("feedbackConvScore") match {
          case Some(t) => Option { t.asInstanceOf[Double] }
          case _ => None : Option[Double]
        }

        val algorithmConvScore : Option[Double] = source.get("algorithmConvScore") match {
          case Some(t) => Option { t.asInstanceOf[Double] }
          case _ => None : Option[Double]
        }

        val feedbackAnswerScore : Option[Double] = source.get("feedbackAnswerScore") match {
          case Some(t) => Option { t.asInstanceOf[Double] }
          case _ => None : Option[Double]
        }

        val algorithmAnswerScore : Option[Double] = source.get("algorithmAnswerScore") match {
          case Some(t) => Option { t.asInstanceOf[Double] }
          case _ => None : Option[Double]
        }

        val start : Boolean = source.get("start") match {
          case Some(t) => t.asInstanceOf[Boolean]
          case _ => false
        }

        val annotationsOut: QADocumentAnnotations = QADocumentAnnotations(
          dclass = dclass,
          doctype = doctype,
          state = state,
          agent = agent,
          escalated = escalated,
          answered = answered,
          triggered = triggered,
          followup = followup,
          feedbackConv = feedbackConv,
          feedbackConvScore = feedbackConvScore,
          algorithmConvScore = algorithmConvScore,
          feedbackAnswerScore = feedbackAnswerScore,
          algorithmAnswerScore = algorithmAnswerScore,
          start = start
        )
        // end annotations

        val document : QADocument = QADocument(
          id = id,
          conversation = conversation,
          indexInConversation = indexInConversation,
          status = status,
          coreData = coreDataOut,
          annotations = annotationsOut,
          timestamp = timestamp
        )

        val searchDocument : SearchQADocument = SearchQADocument(score = item.getScore, document = document)
        searchDocument
      }
    }

    val filteredDoc : List[SearchQADocument] = documents.getOrElse(List[SearchQADocument]())

    val maxScore : Float = searchResp.getHits.getMaxScore
    val totalHits = searchResp.getHits.totalHits
    val total : Int = filteredDoc.length
    val searchResults : SearchQADocumentsResults = SearchQADocumentsResults(totalHits = totalHits,
      hitsCount = total, maxScore = maxScore, hits = filteredDoc)

    val searchResultsOption : Future[Option[SearchQADocumentsResults]] = Future { Option { searchResults } }
    searchResultsOption
  }

  def create(indexName: String, document: QADocument, refresh: Int): Future[Option[IndexDocumentResult]] = Future {
    val builder : XContentBuilder = jsonBuilder().startObject()

    builder.field("id", document.id)
    builder.field("conversation", document.conversation)
    builder.field("index_in_conversation", document.indexInConversation)
    builder.field("status", document.status)

    document.timestamp match {
      case Some(t) => builder.field("timestamp", t)
      case None =>
        builder.field("timestamp", Time.timestampMillis)
    }

    // begin core data
    document.coreData match {
      case Some(coreData) =>
        coreData.question match {
          case Some(t) =>
            builder.field("question", t)
          case _ => ;
        }
        coreData.questionNegative match {
          case Some(t) =>
            val array = builder.startArray("question_negative")
            t.foreach(q => {
              array.startObject().field("query", q).endObject()
            })
            array.endArray()
          case _ => ;
        }
        coreData.questionScoredTerms match {
          case Some(t) =>
            val array = builder.startArray("question_scored_terms")
            t.foreach{case(term, score) =>
              array.startObject().field("term", term)
                .field("score", score).endObject()
            }
            array.endArray()
          case _ => ;
        }
        coreData.answer match {
          case Some(t) =>
            builder.field("answer", t)
          case _ => ;
        }
        coreData.answerScoredTerms match {
          case Some(t) =>
            val array = builder.startArray("answer_scored_terms")
            t.foreach{case(term, score) =>
              array.startObject().field("term", term)
                .field("score", score).endObject()
            }
            array.endArray()
          case _ => ;
        }
        coreData.topics match {
          case Some(t) =>
            builder.field("topics", t)
          case _ => ;
        }
        coreData.verified match {
          case Some(t) =>
            builder.field("verified", t)
          case _ => builder.field("verified", false)

        }
        coreData.done match {
          case Some(t) =>
            builder.field("done", t)
          case _ => builder.field("done", false)
        }
      case _ => QADocumentCore()
    }
    // end core data

    // begin annotations
    document.annotations.dclass match {
      case Some(t) =>
        builder.field("dclass", t)
      case _ => ;
    }
    builder.field("doctype", document.annotations.doctype)
    document.annotations.state match {
      case Some(t) =>
        builder.field("state", t)
      case _ => ;
    }
    builder.field("agent", document.annotations.agent)
    builder.field("escalated", document.annotations.escalated)
    builder.field("answered", document.annotations.answered)
    builder.field("triggered", document.annotations.triggered)
    builder.field("followup", document.annotations.followup)
    document.annotations.feedbackConv match {
      case Some(t) =>
        builder.field("feedbackConv", t)
      case _ => ;
    }
    document.annotations.feedbackConvScore match {
      case Some(t) =>
        builder.field("feedbackConvScore", t)
      case _ => ;
    }
    document.annotations.algorithmConvScore match {
      case Some(t) =>
        builder.field("algorithmConvScore", t)
      case _ => ;
    }
    document.annotations.feedbackAnswerScore match {
      case Some(t) =>
        builder.field("feedbackAnswerScore", t)
      case _ => ;
    }
    document.annotations.algorithmAnswerScore match {
      case Some(t) =>
        builder.field("algorithmAnswerScore", t)
      case _ => ;
    }
    builder.field("start", document.annotations.start)
    // end annotations

    builder.endObject()

    val client: RestHighLevelClient = elasticClient.httpClient

    val indexReq = new IndexRequest()
      .index(Index.indexName(indexName, elasticClient.indexSuffix))
      .`type`(elasticClient.indexMapping)
      .id(document.id)
      .source(builder)

    val response: IndexResponse = client.index(indexReq, RequestOptions.DEFAULT)

    if (refresh =/= 0) {
      val refresh_index = elasticClient.refresh(Index.indexName(indexName, elasticClient.indexSuffix))
      if(refresh_index.failedShardsN > 0) {
        throw QuestionAnswerServiceException("index refresh failed: (" + indexName + ")")
      }
    }

    val doc_result: IndexDocumentResult = IndexDocumentResult(index = response.getIndex,
      dtype = response.getType,
      id = response.getId,
      version = response.getVersion,
      created = response.status === RestStatus.CREATED
    )

    Option {doc_result}
  }

  def update(indexName: String, document: QADocumentUpdate, refresh: Int): UpdateDocumentsResult = {
    val builder : XContentBuilder = jsonBuilder().startObject()

    document.conversation match {
      case Some(t) => builder.field("conversation", t)
      case None => ;
    }

    document.question match {
      case Some(t) => builder.field("question", t)
      case None => ;
    }

    document.questionNegative match {
      case Some(t) =>
        val array = builder.startArray("question_negative")
        t.foreach(q => {
          array.startObject().field("query", q).endObject()
        })
        array.endArray()
      case None => ;
    }

    document.questionScoredTerms match {
      case Some(t) =>
        val array = builder.startArray("question_scored_terms")
        t.foreach{case(term, score) =>
          array.startObject().field("term", term)
            .field("score", score).endObject()
        }
        array.endArray()
      case None => ;
    }

    document.indexInConversation match {
      case Some(t) => builder.field("index_in_conversation", t)
      case None => ;
    }

    document.answer match {
      case Some(t) => builder.field("answer", t)
      case None => ;
    }

    document.answerScoredTerms match {
      case Some(t) =>
        val array = builder.startArray("answer_scored_terms")
        t.foreach{
          case(term, score) =>
            array.startObject().field("term", term).field("score", score).endObject()
        }
        array.endArray()
      case None => ;
    }

    document.verified match {
      case Some(t) => builder.field("verified", t)
      case None => ;
    }

    document.topics match {
      case Some(t) => builder.field("topics", t)
      case None => ;
    }

    document.dclass match {
      case Some(t) => builder.field("dclass", t)
      case None => ;
    }

    document.doctype match {
      case Some(t) => builder.field("doctype", t)
      case None => ;
    }

    document.state match {
      case Some(t) => builder.field("state", t)
      case None => ;
    }

    document.status match {
      case Some(t) => builder.field("status", t)
      case None => ;
    }

    builder.endObject()

    val client: RestHighLevelClient = elasticClient.httpClient

    val bulkRequest = new BulkRequest
    document.id.map { id =>
      val updateReq = new UpdateRequest()
        .index(Index.indexName(indexName, elasticClient.indexSuffix))
        .`type`(elasticClient.indexMapping)
        .doc(builder)
        .id(id)
      bulkRequest.add(updateReq, RequestOptions.DEFAULT)
    }

    val bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT)

    if (refresh =/= 0) {
      val refresh_index = elasticClient.refresh(Index.indexName(indexName, elasticClient.indexSuffix))
      if(refresh_index.failedShardsN > 0) {
        throw QuestionAnswerServiceException("index refresh failed: (" + indexName + ")")
      }
    }

    val listOfDocRes: List[UpdateDocumentResult] = bulkResponse.getItems.map(response => {
      UpdateDocumentResult(index = response.getIndex,
        dtype = response.getType,
        id = response.getId,
        version = response.getVersion,
        created = response.status === RestStatus.CREATED
      )
    }).toList

    UpdateDocumentsResult(data = listOfDocRes)
  }

  def read(indexName: String, ids: List[String]): Option[SearchQADocumentsResults] = {
    val client: RestHighLevelClient = elasticClient.httpClient

    val multigetReq = new MultiGetRequest()
    ids.foreach{id =>
      multigetReq.add(
        new MultiGetRequest.Item(Index.indexName(indexName, elasticClient.indexSuffix), elasticClient.indexSuffix, id)
      )
    }

    val response: MultiGetResponse = client.mget(multigetReq, RequestOptions.DEFAULT)

    val documents : Option[List[SearchQADocument]] = Option { response.getResponses
      .toList.filter((p: MultiGetItemResponse) => p.getResponse.isExists).map { e =>

      val item: GetResponse = e.getResponse

      val id : String = item.getId

      val source : Map[String, Any] = item.getSource.asScala.toMap

      val conversation : String = source.get("conversation") match {
        case Some(t) => t.asInstanceOf[String]
        case None => ""
      }

      val indexInConversation : Option[Int] = source.get("index_in_conversation") match {
        case Some(t) => Option { t.asInstanceOf[Int] }
        case None => None : Option[Int]
      }

      val question : String = source.get("question") match {
        case Some(t) => t.asInstanceOf[String]
        case None => ""
      }

      val questionNegative : Option[List[String]] = source.get("question_negative") match {
        case Some(t) =>
          val res = t.asInstanceOf[java.util.ArrayList[java.util.HashMap[String, String]]]
            .asScala.map(_.getOrDefault("query", None.orNull)).filter(_ =/= None.orNull).toList
          Option { res }
        case None => None: Option[List[String]]
      }

      val questionScoredTerms: Option[List[(String, Double)]] = source.get("question_scored_terms") match {
        case Some(t) =>
          Option {
            t.asInstanceOf[java.util.ArrayList[java.util.HashMap[String, Any]]]
              .asScala.map(_.asScala.toMap)
              .map(term => (term.getOrElse("term", "").asInstanceOf[String],
                term.getOrElse("score", 0.0).asInstanceOf[Double])).filter { case (term, _) => term =/= "" }.toList
          }
        case None => None : Option[List[(String, Double)]]
      }

      val answer : String = source.get("answer") match {
        case Some(t) => t.asInstanceOf[String]
        case None => ""
      }

      val answerScoredTerms: Option[List[(String, Double)]] = source.get("answer_scored_terms") match {
        case Some(t) =>
          Option {
            t.asInstanceOf[java.util.ArrayList[java.util.HashMap[String, Any]]]
              .asScala.map(_.asScala.toMap)
              .map(term => (term.getOrElse("term", "").asInstanceOf[String],
                term.getOrElse("score", 0.0).asInstanceOf[Double]))
              .filter{case(term,_) => term =/= ""}.toList
          }
        case None => None : Option[List[(String, Double)]]
      }

      val verified : Boolean = source.get("verified") match {
        case Some(t) => t.asInstanceOf[Boolean]
        case None => false
      }

      val topics : Option[String] = source.get("topics") match {
        case Some(t) => Option { t.asInstanceOf[String] }
        case None => None : Option[String]
      }

      val dclass : Option[String] = source.get("dclass") match {
        case Some(t) => Option { t.asInstanceOf[String] }
        case None => None : Option[String]
      }

      val doctype : String = source.get("doctype") match {
        case Some(t) => t.asInstanceOf[String]
        case None => Doctypes.NORMAL
      }

      val state : Option[String] = source.get("state") match {
        case Some(t) => Option { t.asInstanceOf[String] }
        case None => None : Option[String]
      }

      val status : Int = source.get("status") match {
        case Some(t) => t.asInstanceOf[Int]
        case None => 0
      }

      val document : QADocument = QADocument(id = id, conversation = conversation,
        indexInConversation = indexInConversation,
        question = question,
        questionNegative = questionNegative,
        questionScoredTerms = questionScoredTerms,
        answer = answer,
        answerScoredTerms = answerScoredTerms,
        verified = verified,
        topics = topics,
        dclass = dclass,
        doctype = doctype,
        state = state,
        status = status)

      val searchDocument : SearchQADocument = SearchQADocument(score = .0f, document = document)
      searchDocument
    }
    }

    val filteredDoc : List[SearchQADocument] = documents.getOrElse(List[SearchQADocument]())

    val maxScore : Float = .0f
    val total : Int = filteredDoc.length
    val searchResults : SearchQADocumentsResults = SearchQADocumentsResults(hitsCount = total, maxScore = maxScore,
      hits = filteredDoc)

    val searchResultsOption : Option[SearchQADocumentsResults] = Option { searchResults }
    searchResultsOption
  }

  def readFuture(indexName: String, ids: List[String]): Future[Option[SearchQADocumentsResults]] = Future {
    read(indexName, ids)
  }

  def updateFuture(indexName: String, id: String, document: QADocumentUpdate, refresh: Int):
  Future[UpdateDocumentResult] = Future {
    update(indexName, id, document, refresh)
  }

  def allDocuments(indexName: String, keepAlive: Long = 60000, size: Int = 100): Iterator[QADocument] = {
    val client: RestHighLevelClient = elasticClient.httpClient

    val sourceReq: SearchSourceBuilder = new SearchSourceBuilder()
      .query(QueryBuilders.matchAllQuery)
      .size(size)

    val searchReq = new SearchRequest(Index.indexName(indexName, elasticClient.indexSuffix))
      .source(sourceReq)
      .types(elasticClient.indexMapping)
      .scroll(new TimeValue(keepAlive))

    var scrollResp: SearchResponse = client.search(searchReq, RequestOptions.DEFAULT)

    val iterator = Iterator.continually{
      val documents = scrollResp.getHits.getHits.toList.map { e =>

        val id : String = e.getId
        val source : Map[String, Any] = e.getSourceAsMap.asScala.toMap

        val conversation : String = source.get("conversation") match {
          case Some(t) => t.asInstanceOf[String]
          case None => ""
        }

        val indexInConversation : Option[Int] = source.get("index_in_conversation") match {
          case Some(t) => Option { t.asInstanceOf[Int] }
          case None => None : Option[Int]
        }

        val question : String = source.get("question") match {
          case Some(t) => t.asInstanceOf[String]
          case None => ""
        }

        val questionNegative : Option[List[String]] = source.get("question_negative") match {
          case Some(t) =>
            val res = t.asInstanceOf[java.util.ArrayList[java.util.HashMap[String, String]]]
              .asScala.map(_.getOrDefault("query", None.orNull)).filter(_ =/= None.orNull).toList
            Option { res }
          case None => None: Option[List[String]]
        }

        val questionScoredTerms: Option[List[(String, Double)]] = source.get("question_scored_terms") match {
          case Some(t) =>
            Option {
              t.asInstanceOf[java.util.ArrayList[java.util.HashMap[String, Any]]]
                .asScala.map(_.asScala.toMap)
                .map(term => (term.getOrElse("term", "").asInstanceOf[String],
                  term.getOrElse("score", 0.0).asInstanceOf[Double])).filter { case (term, _) => term =/= "" }.toList
            }
          case None => None : Option[List[(String, Double)]]
        }

        val answer : String = source.get("answer") match {
          case Some(t) => t.asInstanceOf[String]
          case None => ""
        }

        val answerScoredTerms: Option[List[(String, Double)]] = source.get("answer_scored_terms") match {
          case Some(t) =>
            Option {
              t.asInstanceOf[java.util.ArrayList[java.util.HashMap[String, Any]]]
                .asScala.map(_.asScala.toMap)
                .map(term => (term.getOrElse("term", "").asInstanceOf[String],
                  term.getOrElse("score", 0.0).asInstanceOf[Double]))
                .filter{case(term,_) => term =/= ""}.toList
            }
          case None => None : Option[List[(String, Double)]]
        }

        val verified : Boolean = source.get("verified") match {
          case Some(t) => t.asInstanceOf[Boolean]
          case None => false
        }

        val topics : Option[String] = source.get("topics") match {
          case Some(t) => Option { t.asInstanceOf[String] }
          case None => None : Option[String]
        }

        val dclass : Option[String] = source.get("dclass") match {
          case Some(t) => Option { t.asInstanceOf[String] }
          case None => None : Option[String]
        }

        val doctype : String = source.get("doctype") match {
          case Some(t) => t.asInstanceOf[String]
          case None => Doctypes.NORMAL
        }

        val state : Option[String] = source.get("state") match {
          case Some(t) => Option { t.asInstanceOf[String] }
          case None => None : Option[String]
        }

        val status : Int = source.get("status") match {
          case Some(t) => t.asInstanceOf[Int]
          case None => 0
        }

        QADocument(id = id, conversation = conversation,
          indexInConversation = indexInConversation,
          question = question,
          questionNegative = questionNegative,
          questionScoredTerms = questionScoredTerms,
          answer = answer,
          answerScoredTerms = answerScoredTerms,
          verified = verified,
          topics = topics,
          dclass = dclass,
          doctype = doctype,
          state = state,
          status = status)
      }

      scrollResp = client.search(searchReq, RequestOptions.DEFAULT)
      (documents, documents.nonEmpty)
    }.takeWhile{case (_, docNonEmpty) => docNonEmpty}
    iterator
      .flatMap{case (doc, _) => doc}
  }

  private[this] def extractionReq(text: String, er: UpdateQATermsRequest) = TermsExtractionRequest(text = text,
    tokenizer = Some("space_punctuation"),
    commonOrSpecificSearchPrior = Some(CommonOrSpecificSearch.COMMON),
    commonOrSpecificSearchObserved = Some(CommonOrSpecificSearch.IDXSPECIFIC),
    observedDataSource = Some(ObservedDataSources.KNOWLEDGEBASE),
    fieldsPrior = Some(TermCountFields.all),
    fieldsObserved = Some(TermCountFields.all),
    minWordsPerSentence = Some(10),
    pruneTermsThreshold = Some(100000),
    misspellMaxOccurrence = Some(5),
    activePotentialDecay = Some(10),
    activePotential = Some(true),
    totalInfo = Some(false))

  def updateTextTerms(indexName: String,
                      extractionRequest: UpdateQATermsRequest
                     ): List[UpdateDocumentResult] = {

    val ids: List[String] = List(extractionRequest.id)
    val q = this.read(indexName, ids)
    val hits = q.getOrElse(SearchQADocumentsResults())
    hits.hits.filter(_.document.coreData.nonEmpty).map { hit =>
      hit.document.coreData match {
        case Some(coreData) =>
          val extractionReqQ = extractionReq(text = coreData.question.getOrElse(""), er = extractionRequest)
          val (_, termsQ) = manausTermsExtractionService
            .textTerms(indexName = indexName ,extractionRequest = extractionReqQ)
          val extractionReqA = extractionReq(text = coreData.answer.getOrElse(""), er = extractionRequest)
          val (_, termsA) = manausTermsExtractionService
            .textTerms(indexName = indexName ,extractionRequest = extractionReqA)
          val scoredTermsUpdateReq = QADocumentUpdate(questionScoredTerms = Some(termsQ.toList),
            answerScoredTerms = Some(termsA.toList))
          update(indexName = indexName, id = hit.document.id, document = scoredTermsUpdateReq, refresh = 0)
        case _ =>
          UpdateDocumentResult(index = indexName, dtype = elasticClient.indexSuffix,
            id = hit.document.id, version = -1, created = false)
      }
    }
  }

  def updateTextTermsFuture(indexName: String,
                            extractionRequest: UpdateQATermsRequest):
  Future[List[UpdateDocumentResult]] = Future {
    updateTextTerms(indexName, extractionRequest)
  }

  def updateAllTextTerms(indexName: String,
                         extractionRequest: UpdateQATermsRequest,
                         keepAlive: Long = 3600000): Iterator[UpdateDocumentResult] = {
    allDocuments(indexName = indexName, keepAlive = keepAlive).filter(_.coreData.nonEmpty).map { item =>
      item.coreData match {
        case Some(coreData) =>
          val extractionReqQ = extractionReq(text = coreData.question.getOrElse(""), er = extractionRequest)
          val extractionReqA = extractionReq(text = coreData.answer.getOrElse(""), er = extractionRequest)
          val (_, termsQ) = manausTermsExtractionService
            .textTerms(indexName = indexName ,extractionRequest = extractionReqQ)
          val (_, termsA) = manausTermsExtractionService
            .textTerms(indexName = indexName ,extractionRequest = extractionReqA)
          val scoredTermsUpdateReq = QADocumentUpdate(questionScoredTerms = Some(termsQ.toList),
            answerScoredTerms = Some(termsA.toList))
          update(indexName = indexName, id = item.id, document = scoredTermsUpdateReq, refresh = 0)
        case _ =>
          UpdateDocumentResult(index = indexName, dtype = elasticClient.indexSuffix,
            id = item.id, version = -1, created = false)
      }
    }
  }
}
