package com.getjenny.analyzer.atoms

import com.getjenny.analyzer.expressions.{AnalyzersData, Result}

/**
  * Created by mal on 20/02/2017.
  */

/** Basically a word separated from the others. E.g.:
  * "foo" matches "foo and bar" but not "fooish and bar"
  * "foo.*" matches "pippo and pluto" and "fooish and bar"
  */
class KeywordAtomic(val arguments: List[String], restricted_args: Map[String, String]) extends AbstractAtomic {
  val keyword = arguments.headOption match {
    case Some(t) => t
    case _ => throw ExceptionAtomic("KeywordAtomic: must have one argument")
  }
  override def toString: String = "keyword(\"" + keyword + "\")"
  val isEvaluateNormalized: Boolean = true
  private[this] val rx = {"""\b""" + keyword + """\b"""}.r
  def evaluate(query: String, data: AnalyzersData = AnalyzersData()): Result = {
    val freq = rx.findAllIn(query).toList.length
    val queryLength = """\S+""".r.findAllIn(query).toList.length
    //if (freq > 0) println("DEBUG: KeywordAtomic: '" + keyword + "' found " + freq +
    //  " times in " + query + " (length=" + queryLength + ").")
    val score = if(queryLength.toDouble > 0)
      freq.toDouble / queryLength.toDouble
    else
      0.0
    Result(score = score)
  }

}
