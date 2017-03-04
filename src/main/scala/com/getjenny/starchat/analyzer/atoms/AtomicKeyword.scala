package com.getjenny.starchat.analyzer.atoms

/**
  * Created by mal on 20/02/2017.
  */

/** Basically a word separated from the others. E.g.:
  * "pippo" matches "pippo and pluto" but not "pippone and pluto"
  * "pippo.*" matches "pippo and pluto" and "pippone and pluto"
  */
class AtomicKeyword(val keyword: String) extends AbstractAtomic {
  override def toString: String = "keyword(\"" + keyword + "\")"
  val isEvaluateNormalized: Boolean = true
  private val rx = {"""\b""" + keyword + """\b"""}.r
  def evaluate(query: String): Double = rx.findAllIn(query).toList.length.toDouble / """\S+""".r.findAllIn(query).toList.length
}
