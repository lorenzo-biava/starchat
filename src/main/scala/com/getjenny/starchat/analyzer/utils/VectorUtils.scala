package com.getjenny.starchat.analyzer.utils

/**
  * Created by mal on 20/02/2017.
  */

import breeze.linalg._
import breeze.linalg.functions._

//https://github.com/scalanlp/breeze/wiki/Linear-Algebra-Cheat-Sheet

object VectorUtils {

  def sumArrayOfArrays(vectors: scala.Vector[scala.Vector[Double]]): scala.Vector[Double] = {
    val matrix = DenseMatrix(vectors.map(_.toArray): _*)
    sum(matrix, Axis._0).t.toArray.toVector
  }

  /** Calculate the cosine distance between vectors
    *
    * @param u first vector
    * @param v second vector
    * @return the distance in the range 0.0 - 1.0
    */
  def cosineDist(u: scala.Vector[Double], v: scala.Vector[Double]): Double = {
    if (u.length != v.length) throw new IllegalArgumentException("Vectors have different length")

    /** the range of the function cosineDistance (breeze package) goes from 2.0 (max distance) to 0.0 (min distance)
      * we divide by 2.0 to normalize between 1.0 and 0.0
      */
    cosineDistance(DenseVector(u.toArray), DenseVector(v.toArray)) / 2.0
  }

  def euclideanDist(u: scala.Vector[Double], v: scala.Vector[Double]): Double = {
    if (u.length != v.length) throw new IllegalArgumentException("Vectors have different length")
    euclideanDistance(DenseVector(u.toArray), DenseVector(v.toArray))
  }

}
