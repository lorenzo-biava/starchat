package com.getjenny.starchat.analyzer.atoms
import com.getjenny.analyzer.atoms.AbstractAtomic
import com.getjenny.starchat.analyzer.utils.TextToVectorsTools
import com.getjenny.analyzer.util.VectorUtils._
import com.getjenny.starchat.analyzer.utils.TextToVectorsTools._

import scala.concurrent.{Await, ExecutionContext, Future}
import com.getjenny.starchat.services._
import com.getjenny.analyzer.expressions.{AnalyzersData, Result}

import ExecutionContext.Implicits.global
import scalaz._
import Scalaz._

/**
  * Created by mal on 20/02/2017.
  */

class W2VCosineWordAtomic(arguments: List[String], restricted_args: Map[String, String]) extends AbstractAtomic {
  /**
    * Return the normalized w2vcosine similarity of the nearest word
    */

  val word: String = arguments.head
  override def toString: String = "similar(\"" + word + "\")"

  val termService: TermService.type = TermService

  val indexName: String = restricted_args("index_name")
  val wordVec: (Vector[Double], Double) = TextToVectorsTools.getSumOfVectorsFromText(indexName, word)

  val isEvaluateNormalized: Boolean = true
  def evaluate(query: String, data: AnalyzersData = AnalyzersData()): Result = {
    val textVectors = termService.textToVectors(indexName, query)
    val distance: Double = if (textVectors.nonEmpty && textVectors.get.terms.nonEmpty) {
      val termVector = textVectors.get.terms.get.terms.filter(term => term.vector.nonEmpty)
        .map(term => term.vector.get)
      val distanceList = termVector.map(vector => {
        if(vector.isEmpty || wordVec._2 === 0.0) {
          0.0
        } else {
          1 - cosineDist(vector, wordVec._1)
        }
      })
      val dist = if (distanceList.nonEmpty) distanceList.max else 0.0
      dist
    } else {
      0.0
    }
    Result(score=distance)
  }
  // Similarity is normally the cosine itself. The threshold should be at least
  // angle < pi/2 (cosine > 0), but for synonyms let's put cosine > 0.6, i.e. self.evaluate > 0.8
  override val matchThreshold: Double = 0.8
}
