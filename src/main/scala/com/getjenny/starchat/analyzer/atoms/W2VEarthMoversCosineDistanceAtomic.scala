package com.getjenny.starchat.analyzer.atoms

import com.getjenny.starchat.analyzer.utils.VectorUtils._
import com.getjenny.analyzer.atoms.AbstractAtomic

import scala.concurrent.{Await, ExecutionContext, Future}
import com.getjenny.starchat.services._

import ExecutionContext.Implicits.global

/**
  * Created by angelo on 04/04/17.
  */

class W2VEarthMoversCosineDistanceAtomic(val sentence: String) extends AbstractAtomic  {
  /**
    * cosine distance between sentences renormalized at [0, 1]: (cosine + 1)/2
    *
    * state_lost_password_cosine = Cosine("lost password")
    * state_lost_password_cosine.evaluate("I'm desperate, I've lost my password")
    *
    */

  val termService = new TermService

  val empty_vec = Vector.fill(300){0.0}

  implicit class Crosstable[X](xs: Traversable[X]) {
    def cross[Y](ys: Traversable[Y]) = for { x <- xs; y <- ys } yield (x, y)
  }

  def R_EMD(text1: String, text2: String): Double = {
    val text_vectors1 = termService.textToVectors(text = text1)
    val vectors1 = text_vectors1 match {
      case Some(t) => {
        t.terms.get.terms.map(e => (e.term, e.vector.get))
      }
      case _ => List.empty[(String, Vector[Double])]
    }

    val text_vectors2 = termService.textToVectors(text = text2)
    val vectors2 = text_vectors2 match {
      case Some(t) => {
        t.terms.get.terms.map(e => (e.term, e.vector.get))
      }
      case _ => List.empty[(String, Vector[Double])]
    }

    val words1 = vectors1.groupBy(_._1).map(x =>
      (x._1, (x._2.length.toDouble, x._2.head._2.asInstanceOf[Vector[Double]])))
    val words2 = vectors2.groupBy(_._1).map(x =>
      (x._1, (x._2.length.toDouble, x._2.head._2.asInstanceOf[Vector[Double]])))

    if (words1.isEmpty && words2.isEmpty) {
      Double.PositiveInfinity
    } else if (words1.isEmpty || words2.isEmpty) {
      0.0
    } else {
      val wordlist: Seq[String] = (words1.keys ++ words2.keys).toSet.toSeq
      val weighted_words1 = words1.map(x => (x._1, (x._2._1 / words2.size, x._2._2)))
      val weighted_words2 = words2.map(x => (x._1, (x._2._1 / words1.size, x._2._2)))

      val work_from_v_to_u = weighted_words1.map({ case((term1, (weight1, vector1))) =>
        val min_term = weighted_words2.map({ case((term2, (weight2, vector2))) =>
          val distance: Double = cosineDist(vector1, vector2)
          (term1, term2, weight1, weight2, vector1, vector2, distance, weight1 * distance)
        }).minBy(_._7)
        min_term._8
      }).sum

      val work_from_u_to_v = weighted_words2.map({ case((term1, (weight1, vector1))) =>
        val min_term = weighted_words1.map({ case((term2, (weight2, vector2))) =>
          val distance: Double = cosineDist(vector1, vector2)
          (term1, term2, weight1, weight2, vector1, vector2, distance, weight1 * distance)
        }).minBy(_._7)
        min_term._8
      }).sum

      val dist = math.min(work_from_u_to_v, work_from_v_to_u)
      dist
    }
  }

  override def toString: String = "similarEmd(\"" + sentence + "\")"
  val isEvaluateNormalized: Boolean = true
  def evaluate(query: String): Double = {
    val emd_dist = R_EMD(query, sentence)
    println(emd_dist)
    if (emd_dist == 0) 1.0 else 1.0 / emd_dist
  }

  // Similarity is normally the cosine itself. The threshold should be at least

  // angle < pi/2 (cosine > 0), but for synonyms let's put cosine > 0.6, i.e. self.evaluate > 0.8
  override val match_threshold: Double = 0.8
}