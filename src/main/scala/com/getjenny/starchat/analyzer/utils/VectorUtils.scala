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

  def cosineDist(u: scala.Vector[Double], v: scala.Vector[Double]): Double = {
    if (u.length != v.length) throw new IllegalArgumentException("Vectors have different length")
    cosineDistance(DenseVector(u.toArray), DenseVector(v.toArray)) / 2.0
  }

  def euclideanDist(u: scala.Vector[Double], v: scala.Vector[Double]): Double = {
    if (u.length != v.length) throw new IllegalArgumentException("Vectors have different length")
    euclideanDistance(DenseVector(u.toArray), DenseVector(v.toArray))
  }

}
