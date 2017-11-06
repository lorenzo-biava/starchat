package com.getjenny.analyzer.atoms

import com.getjenny.analyzer.expressions.{AnalyzersData, Result}
import com.getjenny.analyzer.util._

/**
  * Created by angelo on 18/09/2017.
  */

/** calculate the cosine distance between vectors of keywords
  */
class CosineDistanceAnalyzer(val arguments: List[String]) extends AbstractAtomic {
  override def toString: String = "cosDistanceKeywords(\"" + arguments + "\")"
  val isEvaluateNormalized: Boolean = true
  def evaluate(query: String, data: AnalyzersData = AnalyzersData()): Result = {
    // 1- tokenize
    val tokens = query.split("\\W").filter(_ != "")

    // 2- for each argument try to match with the tokens and extract dimensions
    val match_list = arguments.flatMap(keyword => {
      val rx = {"""\b""" + keyword + """\b"""}.r
      val matches = tokens.map(t => {
        val match_list = rx.findAllIn(t).toList
        (keyword, t, match_list.length)
      })
      matches
    })

    val keyword_groups = match_list.groupBy(_._1).map(x => {
      (x._1, x._2.map(c => c._2).toSet.toList, x._2.map(c => c._3).sum)
    }).filter(_._3 < 1).map(x => (x._1, x._2, 1)).toList // remove keywords with matches
    val token_groups = match_list.groupBy(_._2).map(x => {
      (x._1, x._2.map(c => c._1).toSet.toList, x._2.map(c => c._3).sum)
    }).toList

    val analyzer_items = (keyword_groups ::: token_groups).map(x => (x._1, x._3))
    val query_word_count = tokens.map(t => (t,1)).groupBy(_._1).map(x => (x._1, x._2.map(c => c._2).sum))
    val query_terms = analyzer_items.map(x => {
      val occ = query_word_count.getOrElse(x._1, 0)
      (x._1, occ)
    })

    val query_vector = query_terms.map(x => x._2.toDouble).toVector
    val analyzer_vector = analyzer_items.map(x => x._2.toDouble).toVector

    //println("DEBUG: " + analyzer_items)
    //println("DEBUG: " + query_terms)

    val score = 1 - VectorUtils.cosineDist(query_vector, analyzer_vector)

    Result(score = score)
  }
}
