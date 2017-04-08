package com.getjenny.starchat.analyzer.atoms

import com.getjenny.starchat.analyzer.utils.VectorUtils._
import com.getjenny.starchat.analyzer.utils.Emd
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

  //TODO: this analyzer is not complete
  val termService = new TermService

  val empty_vec = Vector.fill(300){0.0}

  implicit class Crosstable[X](xs: Traversable[X]) {
    def cross[Y](ys: Traversable[Y]) = for { x <- xs; y <- ys } yield (x, y)
  }

  def getTextMatrix(text1: String, text2: String):
      (scala.Vector[Double], scala.Vector[Double], Int) = {

    val text_vectors1 = termService.textToVectors(text = text1)
    val vectors1 = text_vectors1 match {
      case Some(t) => {
        t.terms.get.terms.zipWithIndex.map(e => (e._1.term, e._1.vector.get))
      }
      case _ => List.empty[(String, Vector[Double])]
    }

    val text_vectors2 = termService.textToVectors(text = text2)
    val vectors2 = text_vectors2 match {
      case Some(t) => {
        t.terms.get.terms.zipWithIndex.map(e => (e._1.term, e._1.vector.get))
      }
      case _ => List.empty[(String, Vector[Double])]
    }

    val words1 = vectors1.groupBy(_._1).map(x =>
      (x._1, (x._2.length.toDouble, x._2.head._2.asInstanceOf[Vector[Double]])))
    val words2 = vectors2.groupBy(_._1).map(x =>
      (x._1, (x._2.length.toDouble, x._2.head._2.asInstanceOf[Vector[Double]])))

    val wordlist: Seq[String] = (words1.keys ++ words2.keys).toSet.toSeq
    val weighted_words1 = words1.map(x => (x._1, (x._2._1 / words2.size, x._2._2)))
    val weighted_words2 = words2.map(x => (x._1, (x._2._1 / words1.size, x._2._2)))

    /*
    val m1: scala.Vector[scala.Vector[Double]] = wordlist.map(w => {
      val v = words1.getOrElse(w, (0.0, empty_vec))
      val weighted_v = v._2.map(x => x * v._1)
      weighted_v
    }).toVector

    val m2: scala.Vector[scala.Vector[Double]] = wordlist.map(w => {
      val v = words2.getOrElse(w, (0.0, empty_vec))
      val weighted_v = v._2.map(x => x * v._1)
      weighted_v
    }).toVector
    */

    val m1: scala.Vector[Double] = (wordlist cross wordlist).map({ case (t1, t2) =>
      val v1 = weighted_words1.getOrElse(t1, (0.0, empty_vec))
      val v2 = weighted_words2.getOrElse(t2, (0.0, empty_vec))
      val value = if (v1._1 == 0 || v2._1 == 0) 0.0 else euclideanDist(v1._2, v2._2) * v1._1
      value
    }).toVector

   val m2: scala.Vector[Double] = (wordlist cross wordlist).map({ case (t1, t2) =>
      val v1 = weighted_words1.getOrElse(t1, (0.0, empty_vec))
      val v2 = weighted_words2.getOrElse(t2, (0.0, empty_vec))
      val value = if (v1._1 == 0 || v2._1 == 0) 0.0 else euclideanDist(v1._2, v2._2) * v2._1
      value
    }).toVector

   (m1, m2, wordlist.length)
  }

  override def toString: String = "similarEmd(\"" + sentence + "\")"
  val isEvaluateNormalized: Boolean = true
  def evaluate(query: String): Double = {
    val flow = getTextMatrix(query, sentence)
    val emd_dist = Emd.emdDistV(flow._1, flow._2)
    println(emd_dist)
    if (emd_dist == 0) 1.0 else 1.0 / emd_dist
  }

  // Similarity is normally the cosine itself. The threshold should be at least
  // angle < pi/2 (cosine > 0), but for synonyms let's put cosine > 0.6, i.e. self.evaluate > 0.8
  override val match_threshold: Double = 0.8
}