package com.getjenny.starchat.analyzer.utils

/**
  * Created by mal on 20/02/2017.
  */
object Vectors {

  def word2Vec(word: String): Array[Double] = Array.fill(3){math.random }

  def sumArrayOfArrays(a: Array[Array[Double]]): Array[Double] =
    if (a.tail.length == 0) a.head  else a.head.zip(sumArrayOfArrays(a.tail)).map { case (x, y) => x + y }

  def sentence2Vec(sentence: String): Array[Double] = {
    val words = sentence.split("\\s+").map(word2Vec)
    sumArrayOfArrays(words)
  }

  def scalarProductOfArrays(v: Array[Double], u: Array[Double]): Double =
    if (u.length != v.length) throw new IllegalArgumentException("Vectors have different length")
    else u.zip(v).map(x => x._1 * x._2).sum

  def norm(v: Array[Double]): Double = math.sqrt(scalarProductOfArrays(v, v))

  def cosineDistance(v: Array[Double], u: Array[Double]): Double =
    if (u.length != v.length) throw new IllegalArgumentException("Vectors have different length")
    else scalarProductOfArrays(v, u) / (norm(v) * norm(u))
}
