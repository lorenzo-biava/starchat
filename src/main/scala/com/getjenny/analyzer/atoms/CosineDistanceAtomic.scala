package com.getjenny.analyzer.atoms

import com.getjenny.analyzer.expressions.{AnalyzersDataInternal, Result}
import com.getjenny.analyzer.util._
import scalaz._
import Scalaz._

/**
  * Created by angelo on 18/09/2017.
  */

/** calculate the cosine distance between vectors of keywords
  */
class CosineDistanceAnalyzer(val arguments: List[String], restricted_args: Map[String, String]) extends AbstractAtomic {
  override def toString: String = "cosDistanceKeywords(\"" + arguments + "\")"
  val isEvaluateNormalized: Boolean = true
  def evaluate(query: String, data: AnalyzersDataInternal = AnalyzersDataInternal()): Result = {
    // 1- tokenize
    val queryTokens = query.split("\\W").filter(_ =/= "")

    // 2- for each argument try to match with the tokens and extract dimensions
    val matchList = arguments.flatMap(keyword => {
      val rx = {"""\b""" + keyword + """\b"""}.r
      queryTokens.map(token => {
        val tokenMatches = rx.findAllIn(token).toList
        (keyword, token, tokenMatches.length)
      })
    })

    val keywordGroups = matchList.groupBy{case (keyword, _, _) => keyword}.map { // group by keyword
      case (keyword, tokenList) =>
        (
          keyword,
          tokenList.map{case(_, token, _) => token}.distinct, // distinct tokens
          tokenList.map{case(_, _, tokenMatchesLength) => tokenMatchesLength}.sum // num of matches
        )
    }.filter{case (_, _, totalTokenMatches) => totalTokenMatches < 1} // remove keywords with matches
      .map{case(keyword, tokenList, _) => (keyword, tokenList, 1)}.toList

    val tokenGroups = matchList.groupBy{case (_, token, _) => token}
      .map{case(token, tokenList) =>
        (
          token,
          tokenList.map{case(keyword, _, _) => keyword}.distinct,
          tokenList.map{case(_, _, totalTokenMatches) => totalTokenMatches}.sum
        )
      }.toList

    val analyzerItems = (keywordGroups ::: tokenGroups)
      .map{case(item, _, matchCount) => (item, matchCount)}
    val queryWordCount = queryTokens.map{token => (token,1)}
      .groupBy{case(token, _) => token}
      .map{case(token, occurrences) =>
        (token, occurrences.map{case(_, totOccurrences) => totOccurrences}.sum)
      }
    val queryTerms = analyzerItems.map { case (word, _) =>
      val occ = queryWordCount.getOrElse(word, 0)
      (word, occ)
    }

    val queryVector = queryTerms.map{case(_, occ) => occ.toDouble}.toVector
    val analyzerVector = analyzerItems.map{case(_, occ) => occ.toDouble}.toVector

    //println("DEBUG: " + analyzer_items)
    //println("DEBUG: " + query_terms)

    val score = 1 - VectorUtils.cosineDist(queryVector, analyzerVector)

    Result(score = score)
  }
}
