package com.getjenny.starchat.analyzer.utils

/**
  * Created by angelo on 11/04/17.
  */

import com.getjenny.analyzer.util.VectorUtils._
import com.getjenny.starchat.entities._
import com.getjenny.starchat.services._

object TextToVectorsTools {
  val termService: TermService.type = TermService
  val emptyVec: Vector[Double] = Vector.fill(300){0.0}

  implicit class Crosstable[X](xs: Traversable[X]) {
    def cross[Y](ys: Traversable[Y]) = for { x <- xs; y <- ys } yield (x, y)
  }

   def getSumOfTermsVectors(terms: Option[TextTerms]): (Vector[Double], Double) = {
    val textVectors = terms
    val vector = textVectors match {
      case Some(t) => {
        val vectors = t.terms.get.terms.map(e => e.vector.get).toVector
        val sentenceVector =
          if (vectors.nonEmpty) sumArrayOfArrays(vectors) else emptyVec
        val reliabilityFactor =
          textVectors.get.terms_found_n.toDouble / textVectors.get.text_terms_n.toDouble
        (sentenceVector, reliabilityFactor)
      }
      case _ => (emptyVec, 0.0) //default dimension
    }
    vector
  }

  def getSumOfVectorsFromText(index_name: String, text: String): (Vector[Double], Double) = {
    val textVectors = termService.textToVectors(index_name, text)
    val vector = getSumOfTermsVectors(textVectors)
    vector
  }

}
