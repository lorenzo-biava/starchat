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
    "checkMinute"
  )

  override def get(name: String, argument: List[String], restricted_args: Map[String, String]):
                  AbstractAtomic = name.filter(c => !c.isWhitespace ) match {
    case "keyword" => new KeywordAtomic(argument, restricted_args)
    case "regex" => new RegularExpressionAtomic(argument, restricted_args)
    case "search" => new SearchAtomic(argument, restricted_args)
    case "synonym" => new W2VCosineWordAtomic(argument, restricted_args)
    case "similar" => new W2VCosineSentenceAtomic(argument, restricted_args)
    case "similarState" => new W2VCosineStateAtomic(argument, restricted_args)
    case "similarEucEmd" => new W2VEarthMoversEuclideanDistanceAtomic(argument, restricted_args)
    case "similarEucEmdState" => new W2VEarthMoversEuclideanDistanceStateAtomic(argument, restricted_args)
    case "similarCosEmd" => new W2VEarthMoversCosineDistanceAtomic(argument, restricted_args)
    case "similarCosEmdState" => new W2VEarthMoversCosineDistanceStateAtomic(argument, restricted_args)
    case "matchPatternRegex" => new MatchPatternRegexAtomic(argument, restricted_args)
    case "matchDateDDMMYYYY" => new MatchDateDDMMYYYYAtomic(argument, restricted_args)
    case "existsVariable" => new ExistsVariableAtomic(argument, restricted_args)
    case "hasTravState" => new HasTravStateAtomic(argument, restricted_args)
    case "lastTravStateIs" => new LastTravStateIsAtomic(argument, restricted_args)
    case "prevTravStateIs" => new PrevTravStateIsAtomic(argument, restricted_args)
    case ("distance" | "cosDistanceKeywords") => new CosineDistanceAnalyzer(argument, restricted_args)
    case "checkTimestamp" => new CheckTimestampAtomic(argument, restricted_args)
    case "checkDayOfWeek" => new CheckDayOfWeekAtomic(argument, restricted_args)
    case "checkDayOfMonth" => new CheckDayOfMonthAtomic(argument, restricted_args)
    case "checkMonth" => new CheckMonthAtomic(argument, restricted_args)
    case "checkHour" => new CheckHourAtomic(argument, restricted_args)
    case "checkMinute" => new CheckMinuteAtomic(argument, restricted_args)
    case _ => throw ExceptionAtomic("Atom \'" + name + "\' not found")
  }
}
