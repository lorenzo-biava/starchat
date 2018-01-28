package com.getjenny.analyzer.utils

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 23/06/17.
  */

import java.util.regex.PatternSyntaxException

import scala.util.matching._
import scalaz.Scalaz._

/** A generic pattern extraction utility class, it extract named patterns matching a given regex
  *   e.g. the following will match tree numbers separated by semicolumn:
  *     [first,second,third](?:([0-9]+:[0-9]:[0-9]+)
  *   if the regex matches it will create the entries into the dictionary e.g.:
  *     10:11:12 will result in Map("first.0" -> "10", "second.0" -> "11", "third.0" -> "12")
  *     the number at the end of the name is an index incremented for multiple occurrences of the pattern
  *     in the query
  *
  * @param declaration the regular expression in the form [<name0>,..,<nameN>](<regex>)
  */
class PatternExtractionRegex(declaration: String) extends
  PatternExtraction(declaration) {

  val pattern_extraction_field_regex = """(?:\[([\w\d\.\_]{1,256}(?:\s{0,8},\s{0,8}[\w\d\.\_]{1,256})*)\])(\(.*\))""".r
  //"es. [group1,group2, group3]((?:[1-9]+)-(?:[0-9]+)(?: (?:[1-9]+)-(?:[0-9]+))*)"

  val regex_components: Map[String, String] = try {
    declaration match {
      case pattern_extraction_field_regex(groups, regex) =>
        Map[String, String]("groups" -> groups, "regex" -> regex)
      case _ =>
        throw PatternExtractionDeclarationParsingException("Parsing of regular expression specification(" +
          declaration + ")")
    }
  } catch {
    case e: PatternExtractionDeclarationParsingException =>
      val message = "Cannot parse the string with [<groups>+](<regex>): " + e.message
      throw PatternExtractionDeclarationParsingException(message, e)
    case e: PatternExtractionParsingException =>
      val message = "Cannot parse the regular expression: " + e.message
      throw PatternExtractionDeclarationParsingException(message, e)
    case e: PatternExtractionBadSpecificationException =>
      val message = "Bad specification of the pattern matching expression: " + e.message
      throw PatternExtractionDeclarationParsingException(message, e)
  }

  val groups: Array[String] = regex_components.getOrElse("groups", "").split(",")
  val expression_declaration: String = regex_components.getOrElse("regex", "")

  val regular_expression: Regex = try {
    new Regex(expression_declaration, groups: _*)
  } catch {
    case e: PatternSyntaxException =>
      throw PatternExtractionParsingException("Regex parsing exception: Description(" + e.getDescription
        + ") Index(" + e.getIndex + ") Message(" + e.getMessage + ") Pattern(" + e.getPattern + ")", e)
  }

  def evaluate(input: String): Map[String, String] = {
    if (expression_declaration.nonEmpty && groups.nonEmpty) {
      val matchIterator = regular_expression.findAllMatchIn(input)
      if (matchIterator.nonEmpty) {
        val capturedPatterns = matchIterator.map(m => {
          val groupNames = m.groupNames.toList
          val groupCount = m.groupCount
          val extracted_patterns = groupNames.map(gn => (gn, m.group(gn)))
          (groupCount, extracted_patterns)
        }).toList.filter(_._1 === groups.length).zipWithIndex.flatMap(m => {
          m._1._2.map(v => {
            (v._1 + "." + m._2, v._2)
          })
        }).toMap
        capturedPatterns
      } else {
        throw PatternExtractionNoMatchException("No match: regex_declaration(" +
          expression_declaration + ") groups(" + groups + ") input(" + input + ")")
      }
    } else {
      throw PatternExtractionBadSpecificationException("regex_declaration(" +
        expression_declaration + ") or groups(" + groups + ") are empty")
    }
  }
}
