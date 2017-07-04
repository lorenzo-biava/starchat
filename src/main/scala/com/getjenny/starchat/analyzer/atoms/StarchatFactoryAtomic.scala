package com.getjenny.starchat.analyzer.atoms

/**
  * Created by mal on 20/02/2017.
  */

import com.getjenny.analyzer.interfaces._
import com.getjenny.analyzer.atoms._

class StarchatFactoryAtomic extends Factory[String, AbstractAtomic] {

  override val operations = Set("keyword", "regex", "search",
    "synonym", "synonymCosine",
    "similar", "similarState",
    "similarEucEmd", "similarEucEmdState",
    "similarCosEmd", "similarCosEmdState",
    "matchPatternRegex", "matchDateDDMMYYYY"
  )

  override def get(name: String, argument: String):
                  AbstractAtomic = name.filter(c => !c.isWhitespace ) match {
    case "keyword" => new KeywordAtomic(argument)
    case "regex" => new RegularExpressionAtomic(argument)
    case "search" => new SearchAtomic(argument)
    case "synonym" => new W2VCosineWordAtomic(argument)
    case "similar" => new W2VCosineSentenceAtomic(argument)
    case "similarState" => new W2VCosineStateAtomic(argument)
    case "similarEucEmd" => new W2VEarthMoversEuclideanDistanceAtomic(argument)
    case "similarEucEmdState" => new W2VEarthMoversEuclideanDistanceStateAtomic(argument)
    case "similarCosEmd" => new W2VEarthMoversCosineDistanceAtomic(argument)
    case "similarCosEmdState" => new W2VEarthMoversCosineDistanceStateAtomic(argument)
    case "matchPatternRegex" => new MatchPatternRegexAtomic(argument)
    case "matchDateDDMMYYYY" => new MatchDateDDMMYYYYAtomic(argument)
    case _ => throw ExceptionAtomic("Atom \'" + name + "\' not found")
  }
}