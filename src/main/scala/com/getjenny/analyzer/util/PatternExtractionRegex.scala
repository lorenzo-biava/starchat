package com.getjenny.analyzer.utils

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 23/06/17.
  */

import java.util.regex.PatternSyntaxException

import scala.util.matching._

class PatternExtractionRegex(declaration: String) extends
  PatternExtraction(declaration) {

  val pattern_extraction_field_regex = """(?:\[([\w\d\.\_]+(?:\s*,\s*[\w\d\.\_]+)*)\])(\(.*\))""".r
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
      val match_iterator = regular_expression.findAllMatchIn(input)
      if (match_iterator.nonEmpty) {
        val captured_patterns = match_iterator.map(m => {
          val group_names = m.groupNames.toList
          val group_count = m.groupCount
          val extracted_patterns = group_names.map(gn => (gn, m.group(gn)))
          (group_count, extracted_patterns)
        }).toList.filter(_._1 == groups.length).zipWithIndex.flatMap(m => {
          m._1._2.map(v => {
            (v._1 + "." + m._2, v._2)
          })
        }).toMap
        captured_patterns
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
