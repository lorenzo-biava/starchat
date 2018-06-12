package com.getjenny.starchat.analyzer.utils

/**
  * Created by angelo on 11/04/17.
  */

import com.getjenny.analyzer.util.VectorUtils._
import com.getjenny.starchat.entities._
import com.getjenny.starchat.services._

object TextToVectorsTools {
  val termService: TermService.type = TermService
  def emptyVec(length: Int = 300): Vector[Double] = Vector.fill(length){0.0}

  implicit class Crosstable[X](xs: Traversable[X]) {
    def cross[Y](ys: Traversable[Y]): Traversable[(X, Y)] = for { x <- xs; y <- ys } yield (x, y)
  }

  def textTermsToVectors(textTerms: Option[TextTerms]): List[(String, Vector[Double])] = {
    textTerms match {
      case Some(t) => {
        t.terms match {
          case Some(terms) => terms.terms.map(e => (e.term, e.vector.getOrElse(TextToVectorsTools.emptyVec())))
          case _ => List.empty[(String, Vector[Double])]
        }
      }
      case _ => List.empty[(String, Vector[Double])]
    }
  }

  def sumOfTermsVectors(textTerms: Option[TextTerms], length: Int = 300): (Vector[Double], Double) = {
    textTerms match {
      case Some(t) => {
        val vectors = t.terms match {
          case Some(k) => k.terms.map(e => e.vector.getOrElse(emptyVec())).toVector
          case _ => Vector.empty[Vector[Double]]
        }

        val sentenceVector = if (vectors.nonEmpty)
          sumArrayOfArrays(vectors)
        else
          emptyVec(length)
        val reliabilityFactor =
          t.terms_found_n.toDouble / math.max(1, t.text_terms_n.toDouble)
        (sentenceVector, reliabilityFactor)
      }
      case _ => (emptyVec(), 0.0) //default dimension
    }
  }

  def sumOfVectorsFromText(index_name: String, text: String): (Vector[Double], Double) = {
    val textVectors = termService.textToVectors(index_name, text)
    val vector = sumOfTermsVectors(textVectors)
    vector
  }

}
