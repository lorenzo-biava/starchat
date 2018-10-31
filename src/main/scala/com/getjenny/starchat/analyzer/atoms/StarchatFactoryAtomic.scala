package com.getjenny.starchat.analyzer.atoms

/**
  * Created by mal on 20/02/2017.
  */

import com.getjenny.analyzer.interfaces._
import com.getjenny.analyzer.atoms._

class StarchatFactoryAtomic extends AtomicFactoryTrait[List[String], AbstractAtomic, Map[String, String]] {

  override val operations = Set(
    "keyword",
    "regex",
    "search",
    "synonym",
    "synonymCosine",
    "similar",
    "similarState",
    "similarEucEmd",
    "similarEucEmdState",
    "similarCosEmd",
    "similarCosEmdState",
    "matchPatternRegex",
    "matchDateDDMMYYYY",
    "existsVariable",
    "hasTravState",
    "lastTravStateIs",
    "prevTravStateIs",
    "cosDistanceKeywords",
    "distance",
    "checkTimestamp",
    "checkDayOfWeek",
    "checkDayOfMonth",
    "checkMonth",
    "checkHour",
    "checkMinute",
    "double"
  )

  override def get(name: String, argument: List[String], restrictedArgs: Map[String, String]):
                  AbstractAtomic = name.filter(c => !c.isWhitespace ) match {
    case "keyword" => new KeywordAtomic(argument, restrictedArgs)
    case "regex" => new RegularExpressionAtomic(argument, restrictedArgs)
    case "search" => new SearchAtomic(argument, restrictedArgs)
    case "synonym" => new W2VCosineWordAtomic(argument, restrictedArgs)
    case "similar" => new W2VCosineSentenceAtomic(argument, restrictedArgs)
    case "similarState" => new W2VCosineStateAtomic(argument, restrictedArgs)
    case "similarEucEmd" => new W2VEarthMoversEuclideanDistanceAtomic(argument, restrictedArgs)
    case "similarEucEmdState" => new W2VEarthMoversEuclideanDistanceStateAtomic(argument, restrictedArgs)
    case "similarCosEmd" => new W2VEarthMoversCosineDistanceAtomic(argument, restrictedArgs)
    case "similarCosEmdState" => new W2VEarthMoversCosineDistanceStateAtomic(argument, restrictedArgs)
    case "matchPatternRegex" => new MatchPatternRegexAtomic(argument, restrictedArgs)
    case "matchDateDDMMYYYY" => new MatchDateDDMMYYYYAtomic(argument, restrictedArgs)
    case "existsVariable" => new ExistsVariableAtomic(argument, restrictedArgs)
    case "hasTravState" => new HasTravStateAtomic(argument, restrictedArgs)
    case "lastTravStateIs" => new LastTravStateIsAtomic(argument, restrictedArgs)
    case "prevTravStateIs" => new PrevTravStateIsAtomic(argument, restrictedArgs)
    case "distance" | "cosDistanceKeywords" => new CosineDistanceAnalyzer(argument, restrictedArgs)
    case "checkTimestamp" => new CheckTimestampAtomic(argument, restrictedArgs)
    case "checkDayOfWeek" => new CheckDayOfWeekAtomic(argument, restrictedArgs)
    case "checkDayOfMonth" => new CheckDayOfMonthAtomic(argument, restrictedArgs)
    case "checkMonth" => new CheckMonthAtomic(argument, restrictedArgs)
    case "checkHour" => new CheckHourAtomic(argument, restrictedArgs)
    case "checkMinute" => new CheckMinuteAtomic(argument, restrictedArgs)
    case "double" => new DoubleNumberAtomic(argument, restrictedArgs)
    case _ => throw ExceptionAtomic("Atom \'" + name + "\' not found")
  }
}
