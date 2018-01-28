package com.getjenny.analyzer.analyzers

/**
  * Created by mal on 20/02/2017.
  */

import com.getjenny.analyzer.atoms._
import com.getjenny.analyzer.expressions.{AnalyzersData, Expression, Result}
import com.getjenny.analyzer.interfaces.{AtomicFactoryTrait, OperatorFactoryTrait}
import com.getjenny.analyzer.operators._

import scalaz.Scalaz._

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

    def loop(chars: List[Char], indice: Int, parenthesis_balance: List[Int], quoteBalance: Int,
             commandBuffer: String, argument_buffer: String, arguments: List[String],
             commandTree: AbstractOperator):  AbstractOperator = {

      if (indice >= chars.length && chars.nonEmpty) {
        if (quoteBalance < 0)
          throw AnalyzerParsingException("Parsing error: quotes are not balanced")
        else if (parenthesis_balance.sum =/= 0)
          throw AnalyzerParsingException("Parsing error: parenthesis are not balanced")
        else
          commandTree
      } else {
        val justOpenedParenthesis = chars(indice) === '(' && !escape_char(chars, indice) && quoteBalance === 0
        val justClosedParenthesis = chars(indice) === ')' && !escape_char(chars, indice) && quoteBalance === 0

        val newParenthesisBalance: List[Int] = {
          // if a parenthesis is inside double quotes does not count
          if (justOpenedParenthesis) 1 :: parenthesis_balance
          else if (justClosedParenthesis) -1 :: parenthesis_balance
          else parenthesis_balance
        }

        // new_quote_balance > 0 if text in a quotation
        val justOpenedQuote = chars(indice) === '"' && !escape_char(chars, indice) && quoteBalance === 0
        val justClosedQuote = chars(indice) === '"' && !escape_char(chars, indice) && quoteBalance === 1
        val newQuoteBalance: Int = {
          if (justOpenedQuote) 1
          else if (justClosedQuote) 0
          else quoteBalance
        }

        if (newParenthesisBalance.sum < 0 || newQuoteBalance < 0)
          throw AnalyzerParsingException("Parsing error: quotes or parenthesis do not match")

        // Start reading the command
        // If not in quotation and have letter, add to command string accumulator
        // Then, if a parenthesis opens put the string in command
        val newCommandBuffer = if ((chars(indice).isLetter || chars(indice).isWhitespace) && newQuoteBalance === 0)
          (commandBuffer + chars(indice)).filter(c => !c.isWhitespace)
        else if (!justClosedParenthesis) ""
        else commandBuffer.filter(c => !c.isWhitespace)

        // Now read the argument of the command
        // If inside last parenthesis was opened and quotes add to argument
        val argumentAcc =
        if (newParenthesisBalance.head === 1 && newQuoteBalance === 1 && !justOpenedQuote) {
          argument_buffer + chars(indice)
        } else {
          ""
        }

        if (justOpenedParenthesis && operatorFactory.operations(commandBuffer)) {
          // We have just read an operator.
          //println("DEBUG Adding the operator " + command_buffer)
          val operator = try {
            operatorFactory.get(commandBuffer, List())
          } catch {
            case e: NoSuchElementException =>
              throw AnalyzerCommandException("Operator does not exists(" + commandBuffer + ")", e)
            case e: Exception =>
              throw AnalyzerCommandException("Unknown error with operator(" + commandBuffer + ")", e)
          }
          loop(chars, indice + 1, newParenthesisBalance, newQuoteBalance, "", argumentAcc,
            arguments,
            commandTree.add(operator, newParenthesisBalance.sum - 1))
        } else if (!atomicFactory.operations(commandBuffer) && !operatorFactory.operations(commandBuffer) &&
          newParenthesisBalance.head === 1 && justOpenedParenthesis) {
          throw AnalyzerCommandException("Atomic or Operator does not exists(" + commandBuffer + ")")
        } else if (atomicFactory.operations(commandBuffer) && newParenthesisBalance.head === 1 && !justClosedQuote) {
          // We are reading an atomic's argument...
          //println("DEBUG calling loop, without adding an atom, with this command buffer: " + command_buffer + " : " + argument_acc)
          loop(chars, indice + 1, newParenthesisBalance, newQuoteBalance, commandBuffer,
            argumentAcc, arguments, commandTree)
        } else if (atomicFactory.operations(commandBuffer) && justClosedParenthesis) {
          // We have read all the atomic's arguments, add the atomic to the tree
          //println("DEBUG Calling loop, adding the atom: " + command_buffer + ", " + arguments)
          val atomic = try {
            atomicFactory.get(commandBuffer, arguments, restricted_args)
          } catch {
            case e: NoSuchElementException =>
              throw AnalyzerCommandException("Atomic does not exists(" + commandBuffer + ")", e)
            case e: Exception =>
              throw AnalyzerCommandException("Unknown error with Atomic(" + commandBuffer + ")", e)
          }
          loop(chars, indice + 1, newParenthesisBalance, newQuoteBalance, "",
            "", List.empty[String], commandTree.add(atomic, newParenthesisBalance.sum))
        } else if (atomicFactory.operations(commandBuffer) && justClosedQuote && !justClosedParenthesis) {
          // We have read atomic's argument, add the argument to the list
          //println("DEBUG Calling loop, adding argument: " + command_buffer + " <- " + argument_buffer)
          loop(chars, indice + 1, newParenthesisBalance, newQuoteBalance, commandBuffer,
            argumentAcc, arguments ::: List(argument_buffer), commandTree)
        } else {
          //println("DEBUG going to return naked command tree... " + chars.length + " : " + command_buffer)
          if (indice < chars.length - 1) {
            loop(chars, indice + 1, newParenthesisBalance, newQuoteBalance,
              newCommandBuffer, argumentAcc, arguments, commandTree)
          } else {
            if (newParenthesisBalance.sum === 0 && newQuoteBalance === 0) commandTree
            else throw AnalyzerParsingException("gobble_commands: Parenthesis or quotes do not match")
          }
        }
      }
    }
    //adding 2 trailing spaces because we always make a check on char(i -2 )
    loop(chars = "  ".toList ::: commands_string.toList,
      indice = 2,
      parenthesis_balance = List(0),
      quoteBalance = 0,
      commandBuffer = "",
      argument_buffer = "",
      arguments = List.empty[String],
      commandTree = new ConjunctionOperator(List.empty[Expression])
    )

  }

} //end class
