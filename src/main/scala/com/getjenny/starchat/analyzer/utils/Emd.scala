package com.getjenny.starchat.analyzer.utils

/**
  * Created by angelo on 07/04/17.
  */

import com.telmomenezes.jfastemd._

object Emd {
  def getValue(values_map: Vector[Double], x: Int, y: Int, bins: Int): Double = {
      values_map((y * bins) + x)
  }

  def getSignature(values_map: Vector[Double], bins: Int): Signature = {
    // find number of entries in the sparse matrix
    var n: Int = 0
    for (x <- 0 until bins) {
      for (y <- 0 until bins) {
        if (getValue(values_map, x, y, bins) > 0) {
            n += 1
          }
      }
    }

    // compute features and weights
    val features: Array[(Feature2D, Double)] =
    (for { x <- 0 until bins ; y <- 0 until bins } yield (x, y))
      .map({ case (x: Int, y: Int) =>
      val weight: Double = getValue(values_map, x, y, bins)
      if(weight > 0) {
        val feature: Feature2D = new Feature2D(x, y)
        (feature, weight)
      } else (null, weight)
    }).filter(x => x._1 != null).toArray

    val signature: Signature = new Signature()
    signature.setNumberOfFeatures(n)
    signature.setFeatures(features.map({case(feature, weight) => feature}))
    signature.setWeights(features.map({case(feature, weight) => weight}))
    signature
  }

  def emdDist(map1: Vector[Double], map2: Vector[Double], bins: Int): Double = {
    val sig1: Signature = getSignature(map1, bins)
    val sig2: Signature = getSignature(map2, bins)
    val dist: Double = JFastEMD.distance(sig1, sig2, -1)
    dist
  }
}