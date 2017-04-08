package com.getjenny.starchat.analyzer.utils

/**
  * Created by angelo on 07/04/17.
  */

import com.telmomenezes.jfastemd._

object Emd {

  implicit class Crosstable[X](xs: Traversable[X]) {
    def cross[Y](ys: Traversable[Y]) = for { x <- xs; y <- ys } yield (x, y)
  }

  def generateSignatureM(m: Vector[Vector[Double]]): Signature = {
    val features_vector = m.zipWithIndex.flatMap(v => v._1.zipWithIndex.map(e => {(v._2, e._2, e._1)}))
      .filter(e => e._3 != 0).map({ case (x, y, f) => (new Feature2D(x, y), f)})

    val (features, weights) = features_vector.unzip
    val signature: Signature = new Signature()
    signature.setNumberOfFeatures(features_vector.length)
    signature.setFeatures(features.toArray)
    signature.setWeights(weights.toArray)
    signature
  }

  def generateSignatureV(m: Vector[Double]): Signature = {
    val m_dim = math.sqrt(m.length).toInt
    val features_vector =
      (((0 to m_dim).map(x => x.toDouble) cross (0 to m_dim).map(x => x.toDouble))
        .toList zip m).filter(e => e._2 != 0).map({ case ((x, y), f) => (new Feature2D(x, y), f)})

    val (features, weights) = features_vector.unzip
    val signature: Signature = new Signature()
    signature.setNumberOfFeatures(features_vector.length)
    signature.setFeatures(features.toArray)
    signature.setWeights(weights.toArray)
    signature
  }

  def emdDistV(m1: Vector[Double], m2: Vector[Double]): Double = {
    val sig1: Signature = generateSignatureV(m1)
    val sig2: Signature = generateSignatureV(m2)
    val dist: Double = JFastEMD.distance(sig1, sig2, -1)
    dist
  }

  def emdDistM(m1: Vector[Vector[Double]], m2: Vector[Vector[Double]]): Double = {
    val sig1: Signature = generateSignatureM(m1)
    val sig2: Signature = generateSignatureM(m2)
    val dist: Double = JFastEMD.distance(sig1, sig2, -1)
    dist
  }
}