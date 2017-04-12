package com.getjenny.starchat.analyzer.utils

/**
  * Created by angelo on 11/04/17.
  */

import com.getjenny.starchat.analyzer.utils.VectorUtils._
import com.getjenny.analyzer.atoms.AbstractAtomic
import com.getjenny.starchat.entities._

import scala.concurrent.{Await, ExecutionContext, Future}
import com.getjenny.starchat.services._

import scala.concurrent.duration._
import scala.concurrent._
import ExecutionContext.Implicits.global

object TextToVectorsTools {
  val termService = new TermService
  val empty_vec = Vector.fill(300){0.0}

  implicit class Crosstable[X](xs: Traversable[X]) {
    def cross[Y](ys: Traversable[Y]) = for { x <- xs; y <- ys } yield (x, y)
  }

   def getSumOfTermsVectors(terms: Option[TextTerms]): (Vector[Double], Double) = {
    val text_vectors = terms
    val vector = text_vectors match {
      case Some(t) => {
        val vectors = t.terms.get.terms.map(e => e.vector.get).toVector
        val sentence_vector =
          if (vectors.length > 0) sumArrayOfArrays(vectors) else empty_vec
        val reliability_factor =
          text_vectors.get.terms_found_n.toDouble / text_vectors.get.text_terms_n.toDouble
        (sentence_vector, reliability_factor)
      }
      case _ => (empty_vec, 0.0) //default dimension
    }
    vector
  }

  def getSumOfVectorsFromText(text: String): (Vector[Double], Double) = {
    val text_vectors = termService.textToVectors(text)
    val vector = getSumOfTermsVectors(text_vectors)
    vector
  }

}
