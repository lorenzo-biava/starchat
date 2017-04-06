package com.getjenny.starchat.analyzer.atoms

import com.getjenny.starchat.analyzer.utils.VectorUtils._
import com.getjenny.analyzer.atoms.AbstractAtomic
import com.getjenny.starchat.entities._

import scala.concurrent.{Await, ExecutionContext, Future}
import com.getjenny.starchat.services._

import scala.concurrent.duration._
import scala.concurrent._
import ExecutionContext.Implicits.global

/**
  * Created by angelo on 04/04/17.
  */

class W2VEarthMoversDistanceAtomic(val sentence: String) extends AbstractAtomic  {
  /**
    * cosine distance between sentences renormalized at [0, 1]: (cosine + 1)/2
    *
    * state_lost_password_cosine = Cosine("lost password")
    * state_lost_password_cosine.evaluate("I'm desperate, I've lost my password")
    *
    */

  val dtTermService = new TermService

  val empty_vec = Vector.fill(300){0.0}
  def getTextVectors(text: String): Vector[(Int, (String, Vector[Double]))] = {
    val text_vectors = dtTermService.textToVectors(text)
    val vectors = text_vectors match {
      case Some(t) => {
        t.terms.get.terms.zipWithIndex.map(e => (e._2, (e._1.term, e._1.vector.get))).toVector
      }
      case _ => Vector.empty[(Int, (String, Vector[Double]))]  //default dimension
    }
    vectors
  }

  implicit class Crossable[X](xs: Traversable[X]) {
    def cross[Y](ys: Traversable[Y]) = for { x <- xs; y <- ys } yield (x, y)
  }

  val sentence_vectors = getTextVectors(sentence)

  override def toString: String = "similar(\"" + sentence + "\")"
  val isEvaluateNormalized: Boolean = true
  def evaluate(query: String): Double = {
    val query_vectors = getTextVectors(query)
    val distance: Double = if (query_vectors.nonEmpty && sentence_vectors.nonEmpty) {
      val pairs = (query_vectors.zipWithIndex.map(x => (x._2, x._1))
        cross sentence_vectors.zipWithIndex.map(x => (x._2, x._1)))
        .map(x => (x._1._1, (x._1._2, x._2._2)))
        .groupBy{_._1}
      val min_distances = pairs.map(query_t => {
        query_t._2.map(x => euclideanDist(x._2._1._2._2, x._2._2._2._2)).min
      })
      val total_distance: Double = min_distances.toList.sum
      val max_distance: Double = if(total_distance == 0) 1 else 1.0 / total_distance
      max_distance
    } else {
      0.0
    }
    distance
  }

  // Similarity is normally the cosine itself. The threshold should be at least
  // angle < pi/2 (cosine > 0), but for synonyms let's put cosine > 0.6, i.e. self.evaluate > 0.8
  override val match_threshold: Double = 0.8
}