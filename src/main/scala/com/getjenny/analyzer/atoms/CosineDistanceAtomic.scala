package com.getjenny.analyzer.atoms

import com.getjenny.analyzer.expressions.{AnalyzersData, Result}
import com.getjenny.analyzer.util._

/**
  * Created by angelo on 18/09/2017.
  */

/** calculate the cosine distance between vectors of keywords
  */
class CosineDistanceAnalyzer(val arguments: List[String], restricted_args: Map[String, String]) extends AbstractAtomic {
  override def toString: String = "cosDistanceKeywords(\"" + arguments + "\")"
  val isEvaluateNormalized: Boolean = true
  def evaluate(query: String, data: AnalyzersData = AnalyzersData()): Result = {
    // 1- tokenize
    val tokens = query.split("\\W").filter(_ != "")

    // 2- for each argument try to match with the tokens and extract dimensions
    val matchList = arguments.flatMap(keyword => {
      val rx = {"""\b""" + keyword + """\b"""}.r
      val matches = tokens.map(t => {
        val match_list = rx.findAllIn(t).toList
        (keyword, t, match_list.length)
      })
      matches
    })

    val keywordGroups = matchList.groupBy(_._1).map(x => {
      (x._1, x._2.map(c => c._2).distinct, x._2.map(c => c._3).sum)
    }).filter(_._3 < 1).map(x => (x._1, x._2, 1)).toList // remove keywords with matches
    val tokenGroups = matchList.groupBy(_._2).map(x => {
      (x._1, x._2.map(c => c._1).distinct, x._2.map(c => c._3).sum)
    }).toList

    val analyzerItems = (keywordGroups ::: tokenGroups).map(x => (x._1, x._3))
    val queryWordCount = tokens.map(t => (t,1)).groupBy(_._1).map(x => (x._1, x._2.map(c => c._2).sum))
    val queryTerms = analyzerItems.map { case (word, _) =>
      val occ = queryWordCount.getOrElse(word, 0)
      (word, occ)
    }

    val queryVector = queryTerms.map(x => x._2.toDouble).toVector
    val analyzerVector = analyzerItems.map(x => x._2.toDouble).toVector

    //println("DEBUG: " + analyzer_items)
    //println("DEBUG: " + query_terms)

    val score = 1 - VectorUtils.cosineDist(queryVector, analyzerVector)

    Result(score = score)
  }
}
