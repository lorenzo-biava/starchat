package com.getjenny.starchat.analyzer.utils

import breeze.linalg.{DenseMatrix, Matrix}
import com.getjenny.analyzer.util.VectorUtils._
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

object EmDistance {
  val termService = TermService

  val emptyVec = Vector.fill(300){0.0}

  implicit class Crosstable[X](xs: Traversable[X]) {
    def cross[Y](ys: Traversable[Y]) = for { x <- xs; y <- ys } yield (x, y)
  }

  //reduced EMD
  def distance(textTerms1: Option[TextTerms], textTerms2: Option[TextTerms],
               dist_f: (Vector[Double], Vector[Double]) => Double): (Double, Double, Double) = {

    val vectors1 = textTerms1 match {
      case Some(t) => {
        t.terms.get.terms.map(e => (e.term, e.vector.get))
      }
      case _ => List.empty[(String, Vector[Double])]
    }

    val vectors2 = textTerms2 match {
      case Some(t) => {
        t.terms.get.terms.map(e => (e.term, e.vector.get))
      }
      case _ => List.empty[(String, Vector[Double])]
    }

    val reliabilityFactor1 = if(textTerms1.nonEmpty) {
      textTerms1.get.terms_found_n.toDouble / textTerms1.get.text_terms_n.toDouble
    } else 0.0

    val reliabilityFactor2 = if(textTerms2.nonEmpty) {
      textTerms2.get.terms_found_n.toDouble / textTerms2.get.text_terms_n.toDouble
    } else 0.0

    val words1 = vectors1.groupBy(_._1).map(x =>
      (x._1, (x._2.length.toDouble, x._2.head._2.asInstanceOf[Vector[Double]])))
    val words2 = vectors2.groupBy(_._1).map(x =>
      (x._1, (x._2.length.toDouble, x._2.head._2.asInstanceOf[Vector[Double]])))

    if (words1.isEmpty && words2.isEmpty) {
      (1.0, 1.0, 1.0)
    } else if (words1.isEmpty || words2.isEmpty) {
      (0.0, 0.0, 0.0)
    } else {
      val weightedWords1 = words1.map(x => (x._1, (x._2._1 / words2.size, x._2._2)))
      val weightedWords2 = words2.map(x => (x._1, (x._2._1 / words1.size, x._2._2)))

      val work_from_v_to_u = weightedWords1.map({ case((term1, (weight1, vector1))) =>
        val min_term = weightedWords2.map({ case((term2, (weight2, vector2))) =>
          val distance: Double = dist_f(vector1, vector2)
          (term1, term2, weight1, weight2, vector1, vector2, distance, weight1 * distance)
        }).minBy(_._7)
        min_term._8
      }).map(x => math.abs(x)).sum

      val workFromUtoV = weightedWords2.map({ case((term1, (weight1, vector1))) =>
        val min_term = weightedWords1.map({ case((term2, (weight2, vector2))) =>
          val distance: Double = dist_f(vector1, vector2)
          (term1, term2, weight1, weight2, vector1, vector2, distance, weight1 * distance)
        }).minBy(_._7)
        min_term._8
      }).map(x => math.abs(x)).sum

      val dist = math.max(workFromUtoV, work_from_v_to_u)
      println("Info: work_from_u_to_v("
        + workFromUtoV + ") work_from_v_to_u(" + work_from_v_to_u + ") dist(" + dist + ")"
        + " reliability_factor1(" + reliabilityFactor1 + ")"
        + " reliability_factor2(" + reliabilityFactor2 + ")"
      )
      (dist, reliabilityFactor1, reliabilityFactor2)
    }
  }

  //reduced EMD
  def distanceText(index_name: String, text1: String, text2: String,
                   dist_f: (Vector[Double], Vector[Double]) => Double): (Double, Double, Double) = {
    val textVectors1 = termService.textToVectors(index_name, text = text1)
    val textVectors2 = termService.textToVectors(index_name, text = text2)
    distance(textVectors1, textVectors2, dist_f)
  }

  def distanceEuclidean(index_name: String, text1: String, text2: String): Double = {
    val emdDist = distanceText(index_name =  index_name, text1 = text1, text2 = text2, euclideanDist)
    val score = (1.0 / (1 + emdDist._1)) * (emdDist._2 * emdDist._3)
    score
  }

  def distanceCosine(index_name: String, text1: String, text2: String): Double = {
    val emdDist = distanceText(index_name = index_name, text1 = text1, text2 = text2, cosineDist)
    val score = (1.0 / (1 + emdDist._1)) * (emdDist._2 * emdDist._3)
    score
  }

  def distanceEuclidean(textTerms1: Option[TextTerms], textTerms2: Option[TextTerms]): Double = {
    val emdDist = distance(textTerms1, textTerms2, euclideanDist)
    val score = (1.0 / (1 + emdDist._1)) * (emdDist._2 * emdDist._3)
    score
  }

  def distanceCosine(textTerms1: Option[TextTerms], textTerms2: Option[TextTerms]): Double = {
    val emdDist = distance(textTerms1, textTerms2, cosineDist)
    val score = (1.0 / (1 + emdDist._1)) * (emdDist._2 * emdDist._3)
    score
  }

}