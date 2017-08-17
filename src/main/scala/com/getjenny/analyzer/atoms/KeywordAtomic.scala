package com.getjenny.analyzer.atoms

import com.getjenny.analyzer.expressions.{Data, Result}

/**
  * Created by mal on 20/02/2017.
  */

/** Basically a word separated from the others. E.g.:
  * "pippo" matches "pippo and pluto" but not "pippone and pluto"
  * "pippo.*" matches "pippo and pluto" and "pippone and pluto"
  */
class KeywordAtomic(val keyword: String) extends AbstractAtomic {
  override def toString: String = "keyword(\"" + keyword + "\")"
  val isEvaluateNormalized: Boolean = true
  private val rx = {"""\b""" + keyword + """\b"""}.r
  def evaluate(query: String, data: Data = Data()): Result = {
    val freq = rx.findAllIn(query).toList.length
    val query_length = """\S+""".r.findAllIn(query).toList.length
    if (freq > 0) println("DEBUG: KeywordAtomic: '" + keyword + "' found " + freq +
      " times in " + query + " (length=" + query_length + ").")
    val score = if(query_length.toDouble > 0)
      freq.toDouble / query_length.toDouble
    else
      0.0
    Result(score = score)
  }

}
