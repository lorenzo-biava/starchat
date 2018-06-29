package com.getjenny.analyzer.util

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 23/06/17.
  */

import java.util.regex.PatternSyntaxException

import scala.util.{Try, Success, Failure}
import scala.util.control.NonFatal
import scala.util.matching._
import scalaz.Scalaz._

/** A generic pattern extraction utility class, it extract named patterns matching a given regex
  *   e.g. the following will match tree numbers separated by semicolumn:
  *     [first,second,third](?:([0-9]+):([0-9]+):([0-9]+))
  *   if the regex matches it will create the entries into the dictionary e.g.:
  *     10:11:12 will result in Map("first.0" -> "10", "second.0" -> "11", "third.0" -> "12")
  *     the number at the end of the name is an index incremented for multiple occurrences of the pattern
  *     in the query
  *
  * @param declaration the regular expression in the form [<name0>,..,<nameN>](<regex>)
  */
class PatternExtractionRegex(declaration: String) extends
  PatternExtraction(declaration) {

  val patternExtractionFieldRegex: Regex =
    """(?:\[([\w\d\.\_]{1,256}(?:\s{0,8},\s{0,8}[\w\d\.\_]{1,256})*)\])(\(.*\))""".r
  //"es. [group1,group2, group3]((?:[1-9]+)-(?:[0-9]+)(?: (?:[1-9]+)-(?:[0-9]+))*)"

  val regexComponents: Try[Map[String, String]] = Try {
    declaration match {
      case patternExtractionFieldRegex(groups, regex) =>
        Map[String, String]("groups" -> groups, "regex" -> regex)
      case _ =>
        throw PatternExtractionDeclarationParsingException("Parsing of regular expression specification(" +
          declaration + ")")
    }
  } recover {
    case e: PatternExtractionDeclarationParsingException =>
      val message = "Cannot parse the string with [<groups>+](<regex>): " + e.message
      throw PatternExtractionDeclarationParsingException(message, e)
    case e: PatternExtractionParsingException =>
      val message = "Cannot parse the regular expression: " + e.message
      throw PatternExtractionDeclarationParsingException(message, e)
    case e: PatternExtractionBadSpecificationException =>
      val message = "Bad specification of the pattern matching expression: " + e.message
      throw PatternExtractionDeclarationParsingException(message, e)
    case NonFatal(e) =>
      val message = "Unknown error matching expression: " + e.getMessage
      throw PatternExtractionDeclarationParsingException(message, e)
  }

  val groups: Array[String] = regexComponents match {
    case Success(g) => g.getOrElse("groups", "").split(",")
    case Failure(e) =>
      val message = "Bad group results: " + e.getMessage
      throw PatternExtractionDeclarationParsingException(message)
  }

  val expressionDeclaration: String = regexComponents match {
    case Success(s) => s.getOrElse("regex", "")
    case Failure(e) =>
      val message = "Bad regex results: " + e.getMessage
      throw PatternExtractionDeclarationParsingException(message)
  }

  val regularExpression: Try[Regex] = Try(new Regex(expressionDeclaration, groups: _*)) recover {
    case e: PatternSyntaxException =>
      throw PatternExtractionParsingException("Regex parsing exception: Description(" + e.getDescription
        + ") Index(" + e.getIndex + ") Message(" + e.getMessage + ") Pattern(" + e.getPattern + ")", e)
    case NonFatal(e) =>
      throw PatternExtractionParsingException("Non fatal exception matching regex: " + e.getMessage)
  }

  def evaluate(input: String): Map[String, String] = {
    if (expressionDeclaration.nonEmpty && groups.nonEmpty) {
      val matchIterator = regularExpression match {
        case Success(regex) => regex.findAllMatchIn(input)
        case Failure(_) => Iterator.empty
      }

      if (matchIterator.nonEmpty) {
        val capturedPatterns = matchIterator.map(m => {
          val groupNames = m.groupNames.toList
          val groupCount = m.groupCount
          val extractedPatterns = groupNames.map(gn => (gn, m.group(gn)))
          (groupCount, extractedPatterns)
        }).toList.filter {
          case (groupCount, _) => groupCount === groups.length
        }.zipWithIndex.flatMap {
          case (matchPair, index) =>
            matchPair._2.map { case (matchKey, matchValue) =>
              (matchKey + "." + index, matchValue)
            }
        }.toMap
        capturedPatterns
      } else {
        throw PatternExtractionNoMatchException("No match: regex_declaration(" +
          expressionDeclaration + ") groups(" + groups + ") input(" + input + ")")
      }
    } else {
      throw PatternExtractionBadSpecificationException("regex_declaration(" +
        expressionDeclaration + ") or groups(" + groups + ") are empty")
    }
  }
}
