package com.getjenny.analyzer.analyzers

/**
  * Created by mal on 20/02/2017.
  */

import com.getjenny.analyzer.operators._
import com.getjenny.analyzer.atoms._
import com.getjenny.analyzer.expressions.{AnalyzersData, Expression, Result}
import com.getjenny.analyzer.interfaces.{AtomicFactoryTrait, OperatorFactoryTrait}
import scalaz._
import Scalaz._

/**
  * All sentences with more than 22 characters and with keywords "password" and either "lost" or "forgot"
  *
  * and(regex(".{22,}"), and(or(keyword("forgot"), keyword("lost")), keyword("password")))
  *
  * but also:
  *
  * or(similar("lost password"), similar("forgot password"))
  *
  * In the latter case "or" is treated as disjunction of probabilities
  */
abstract class DefaultParser(command_string: String, restricted_args: Map[String, String]) extends AbstractParser(command_string: String) {

  val atomicFactory: AtomicFactoryTrait[List[String], AbstractAtomic, Map[String, String]]
  val operatorFactory: OperatorFactoryTrait[List[Expression], AbstractOperator]

  private val operator = this.gobble_commands(command_string)

  override def toString: String = operator.toString
  /** Read a sentence and produce a score (the higher, the more confident)
    */
  def evaluate(sentence: String, data: AnalyzersData = AnalyzersData()): Result = {
    val res = operator.evaluate(query = sentence, data = data)
    if (res.score > 0) println("DEBUG: DefaultParser: '" + this + "' evaluated to " + res)
    res
  }

  /**Produces nested operators
    *
    * e.g.
    *
    * gobble_commands("""boolean-and(regex(".{22,}"), boolean-and(boolean-or(keyword("forgot"), keyword("lost")), keyword("password")))""")
    *
    * will produce a boolean OR operator with inside a regex Expression (which can be evaluated),
    * a boolean AND etc
    *
    */
  def gobble_commands(commands_string: String): AbstractOperator = {

    /** \( does not count, \\( does
      */
    def escape_char(chars: List[Char], i: Int): Boolean = chars(i-1) === '\\' && chars(i-2) =/= '\\'

    def loop(chars: List[Char], indice: Int, parenthesis_balance: List[Int], quote_balance: Int,
             command_buffer: String, argument_buffer: String, arguments: List[String],
             command_tree: AbstractOperator):  AbstractOperator = {

      if (indice >= chars.length && chars.nonEmpty) {
        if (quote_balance < 0)
          throw AnalyzerParsingException("Parsing error: quotes are not balanced")
        else if (parenthesis_balance.sum =/= 0)
          throw AnalyzerParsingException("Parsing error: parenthesis are not balanced")
        else
          command_tree
      } else {
        val just_opened_parenthesis = chars(indice) === '(' && !escape_char(chars, indice) && quote_balance === 0
        val just_closed_parenthesis = chars(indice) === ')' && !escape_char(chars, indice) && quote_balance === 0

        val new_parenthesis_balance: List[Int] = {
          // if a parenthesis is inside double quotes does not count
          if (just_opened_parenthesis) 1 :: parenthesis_balance
          else if (just_closed_parenthesis) -1 :: parenthesis_balance
          else parenthesis_balance
        }

        // new_quote_balance > 0 if text in a quotation
        val just_opened_quote = chars(indice) === '"' && !escape_char(chars, indice) && quote_balance === 0
        val just_closed_quote = chars(indice) === '"' && !escape_char(chars, indice) && quote_balance === 1
        val new_quote_balance: Int = {
          if (just_opened_quote) 1
          else if (just_closed_quote) 0
          else quote_balance
        }

        if (new_parenthesis_balance.sum < 0 || new_quote_balance < 0)
          throw AnalyzerParsingException("Parsing error: quotes or parenthesis do not match")

        // Start reading the command
        // If not in quotation and have letter, add to command string accumulator
        // Then, if a parenthesis opens put the string in command
        val newCommandBuffer = if ((chars(indice).isLetter || chars(indice).isWhitespace) && new_quote_balance === 0)
          (command_buffer + chars(indice)).filter(c => !c.isWhitespace)
        else if (!just_closed_parenthesis) ""
        else command_buffer.filter(c => !c.isWhitespace)

        // Now read the argument of the command
        // If inside last parenthesis was opened and quotes add to argument
        val argument_acc =
        if (new_parenthesis_balance.head == 1 && new_quote_balance == 1 && !just_opened_quote) {
          argument_buffer + chars(indice)
        } else {
          ""
        }

        if (just_opened_parenthesis && operatorFactory.operations(command_buffer)) {
          // We have just read an operator.
          //println("DEBUG Adding the operator " + command_buffer)
          val operator = try {
            operatorFactory.get(command_buffer, List())
          } catch {
            case e: NoSuchElementException =>
              throw AnalyzerCommandException("Operator does not exists(" + command_buffer + ")", e)
            case e: Exception =>
              throw AnalyzerCommandException("Unknown error with operator(" + command_buffer + ")", e)
          }
          loop(chars, indice + 1, new_parenthesis_balance, new_quote_balance, "", argument_acc,
            arguments,
            command_tree.add(operator, new_parenthesis_balance.sum - 1))
        } else if (!atomicFactory.operations(command_buffer) && !operatorFactory.operations(command_buffer) &&
          new_parenthesis_balance.head == 1 && just_opened_parenthesis) {
          throw AnalyzerCommandException("Atomic or Operator does not exists(" + command_buffer + ")")
        } else if (atomicFactory.operations(command_buffer) && new_parenthesis_balance.head == 1 && !just_closed_quote) {
          // We are reading an atomic's argument...
          //println("DEBUG calling loop, without adding an atom, with this command buffer: " + command_buffer + " : " + argument_acc)
          loop(chars, indice + 1, new_parenthesis_balance, new_quote_balance, command_buffer,
            argument_acc, arguments, command_tree)
        } else if (atomicFactory.operations(command_buffer) && just_closed_parenthesis) {
          // We have read all the atomic's arguments, add the atomic to the tree
          //println("DEBUG Calling loop, adding the atom: " + command_buffer + ", " + arguments)
          val atomic = try {
            atomicFactory.get(command_buffer, arguments, restricted_args)
          } catch {
            case e: NoSuchElementException =>
              throw AnalyzerCommandException("Atomic does not exists(" + command_buffer + ")", e)
            case e: Exception =>
              throw AnalyzerCommandException("Unknown error with Atomic(" + command_buffer + ")", e)
          }
          loop(chars, indice + 1, new_parenthesis_balance, new_quote_balance, "",
            "", List.empty[String], command_tree.add(atomic, new_parenthesis_balance.sum))
        } else if (atomicFactory.operations(command_buffer) && just_closed_quote && !just_closed_parenthesis) {
          // We have read atomic's argument, add the argument to the list
          //println("DEBUG Calling loop, adding argument: " + command_buffer + " <- " + argument_buffer)
          loop(chars, indice + 1, new_parenthesis_balance, new_quote_balance, command_buffer,
            argument_acc, arguments ::: List(argument_buffer), command_tree)
        } else {
          //println("DEBUG going to return naked command tree... " + chars.length + " : " + command_buffer)
          if (indice < chars.length - 1) {
            loop(chars, indice + 1, new_parenthesis_balance, new_quote_balance,
              newCommandBuffer, argument_acc, arguments, command_tree)
          } else {
            if (new_parenthesis_balance.sum == 0 && new_quote_balance == 0) command_tree
            else throw AnalyzerParsingException("gobble_commands: Parenthesis or quotes do not match")
          }
        }
      }
    }
    //adding 2 trailing spaces because we always make a check on char(i -2 )
    loop(chars = "  ".toList ::: commands_string.toList,
      indice = 2,
      parenthesis_balance = List(0),
      quote_balance = 0,
      command_buffer = "",
      argument_buffer = "",
      arguments = List.empty[String],
      command_tree = new ConjunctionOperator(List.empty[Expression])
    )

  }

} //end class
