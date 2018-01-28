package com.getjenny.starchat.analyzer.atoms

import com.getjenny.analyzer.analyzers._
import com.getjenny.analyzer.atoms.{AbstractAtomic, ExceptionAtomic}
import com.getjenny.analyzer.expressions.{AnalyzersData, Result}
import com.getjenny.starchat.analyzer.utils.EmDistance
import com.getjenny.starchat.entities.TextTerms
import com.getjenny.starchat.services._

/**
  * Created by angelo on 11/04/17.
  */

class W2VEarthMoversCosineDistanceStateAtomic(val arguments: List[String], restricted_args: Map[String, String])
  extends AbstractAtomic  {
  /**
    * cosine distance between sentences renormalized at [0, 1]: (cosine + 1)/2
    *
    * state_lost_password_cosine = Cosine("lost password")
    * state_lost_password_cosine.evaluate("I'm desperate, I've lost my password")
    *
    */

  val state: String = arguments.headOption match {
    case Some(t) => t
    case _ =>
      throw ExceptionAtomic("similarCosEmdState requires an argument")
  }

  val termService: TermService.type = TermService

  implicit class Crosstable[X](xs: Traversable[X]) {
    def cross[Y](ys: Traversable[Y]) = for { x <- xs; y <- ys } yield (x, y)
  }

  override def toString: String = "similarCosEmdState(\"" + state + "\")"

  val analyzerService: AnalyzerService.type = AnalyzerService

  val indexName = restricted_args("index_name")

  val queriesSentences: Option[DecisionTableRuntimeItem] =
    AnalyzerService.analyzersMap(indexName).analyzerMap.get(state)
  if (queriesSentences.isEmpty) {
    val message = toString + " : state not found on states map"
    analyzerService.log.error(message)
    throw AnalyzerInitializationException(message)
  } else {
    analyzerService.log.info(toString + " : initialized")
  }

  val queriesVectors: List[Option[TextTerms]] = queriesSentences.get.queries.map(item => Option{item})

  val isEvaluateNormalized: Boolean = true
  def evaluate(query: String, data: AnalyzersData = AnalyzersData()): Result = {
    val queryVectors = termService.textToVectors(indexName = indexName, text = query)
    val emdDistQueries = queriesVectors.map(q => {
      val dist = EmDistance.distanceCosine(q , queryVectors)
      dist
    })

    val emdDist = if (emdDistQueries.nonEmpty) emdDistQueries.max else 0.0
    Result(score=emdDist)
  }

  // Similarity is normally the cosine itself. The threshold should be at least

  // angle < pi/2 (cosine > 0), but for synonyms let's put cosine > 0.6, i.e. self.evaluate > 0.8
  override val matchThreshold: Double = 0.8
}
