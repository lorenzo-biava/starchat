package com.getjenny.starchat.analyzer.analyzers

/**
  * Created by mal on 20/02/2017.
  */

import com.getjenny.starchat.analyzer.operators._
import com.getjenny.starchat.analyzer.atoms._

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
class DefaultAnalyzer(command_string: String) extends AbstractAnalyzer(command_string: String) {

  private val logical_operators =  List("and", "or", "disjunction", "conjunction")
  private val operator = this.gobble_commands(command_string)

  override def toString: String = operator.toString
  /** Read a sentence and produce a score (the higher, the more confident)
    */
  def evaluate(sentence: String): Double = {
    operator.evaluate(sentence)
  }

  /**Produces a List for building the tree.
    * make_command_tree("""and(regex(".{22,}"), and(or(keyword("forgot"), keyword("lost")), keyword("password")))""")

  List(
        ("keyword", "password", 3),
        ("keyword", "forgot", 4),
        ("or", "", 3),
        ("and", "", 2),
        ("regex", ".{22,}", 2),
        ("and", "", 1)
      )

    */
  def gobble_commands(commands_string: String): AbstractOperator = {

    /** \( does not count, \\( does
      */
    def escape_char(chars: List[Char], i: Int): Boolean = chars(i-1) == '\\' && chars(i-2) != '\\'

    def loop(chars: List[Char], indice: Int, parenthesis_balance: List[Int], quote_balance: Int,
             command_buffer: String, argument_buffer: String, command_tree: AbstractOperator):  AbstractOperator = {

      val just_opened_parenthesis = chars(indice) == '(' && !escape_char(chars, indice) && quote_balance == 0
      val just_closed_parenthesis = chars(indice) == ')' && !escape_char(chars, indice) && quote_balance == 0

      val new_parenthesis_balance: List[Int] = {
        // if a parenthesis is inside double quotes does not count
        if (just_opened_parenthesis) 1 :: parenthesis_balance
        else if (just_closed_parenthesis) -1 :: parenthesis_balance
        else parenthesis_balance
      }

      // new_quote_balance > 0 if text in a quotation
      val just_opened_quote = chars(indice) == '"' && !escape_char(chars, indice) && quote_balance == 0
      val just_closed_quote = chars(indice) == '"' && !escape_char(chars, indice) && quote_balance == 1
      val new_quote_balance: Int = {
        if (just_opened_quote) 1
        else if  (just_closed_quote) 0
        else quote_balance
      }

      if (new_parenthesis_balance.sum < 0 || new_quote_balance < 0)
        throw new AnalyzerParsingException("Parsing error: quotes do not match")

      // Start reading the command
      // If not in quotation and have letter, add to command string accumulator
      // Then, if a parenthesis opens put the string in command
      val new_command_buffer = if ((chars(indice).isLetter || chars(indice).isWhitespace) && new_quote_balance == 0)
        (command_buffer + chars(indice)).filter(c => !c.isWhitespace )
      else if (!just_closed_parenthesis) ""
      else command_buffer.filter(c => !c.isWhitespace )

      // Now read the argument of the command
      // If inside last parenthesis was opened and quotes add to argument
      val argument_acc = if (new_parenthesis_balance.head == 1 && new_quote_balance == 1  && !just_opened_quote)
        argument_buffer + chars(indice) else ""

      if (just_opened_parenthesis && logical_operators.toSet(command_buffer)) {
        //println("DEBUG Adding the operator " + command_buffer)
        //TODO: better whenever possible
        command_buffer match {
          case "or" =>
            loop(chars, indice + 1, new_parenthesis_balance, new_quote_balance, "", argument_acc,
              command_tree.add(new OrOperator(List()), new_parenthesis_balance.sum - 1))
          case "and" =>
            loop(chars, indice + 1, new_parenthesis_balance, new_quote_balance, "", argument_acc,
              command_tree.add(new AndOperator(List()), new_parenthesis_balance.sum - 1))
          case "conjunction" =>
            loop(chars, indice + 1, new_parenthesis_balance, new_quote_balance, "", argument_acc,
              command_tree.add(new ConjunctionOperator(List()), new_parenthesis_balance.sum - 1))
          case "disjunction" =>
            loop(chars, indice + 1, new_parenthesis_balance, new_quote_balance, "", argument_acc,
              command_tree.add(new DisjunctionOperator(List()), new_parenthesis_balance.sum - 1))
          case _ =>  //TODO: fixme, this exception is never raised because of logical_operators.toSet(command_buffer)
            throw new AnalyzerCommandException("Operator '" + command_buffer + "' not yet implemented....")
        }
      } else if (AtomicFactory.atoms(command_buffer) && new_parenthesis_balance.head == 1 && !just_closed_quote) {
        //println("DEBUG calling loop, without adding an atom, with this command buffer: " + command_buffer)
        loop(chars, indice + 1, new_parenthesis_balance, new_quote_balance, command_buffer,
          argument_acc, command_tree)
      } else if (AtomicFactory.atoms(command_buffer) && just_closed_quote) {
        //println("DEBUG Calling loop, adding the atom " + AtomicFactory.returnAtoms(command_buffer, argument_buffer))
        loop(chars, indice + 1, new_parenthesis_balance, new_quote_balance, command_buffer, argument_acc,
          command_tree.add(AtomicFactory.returnAtoms(command_buffer, argument_buffer),
            new_parenthesis_balance.sum - 1))
      } else {
        //println("DEBUG going to return naked command tree... " + chars.length)
        if (indice < chars.length - 1) loop(chars, indice+1, new_parenthesis_balance, new_quote_balance,
          new_command_buffer, argument_acc, command_tree)
        else {
          if (new_parenthesis_balance.sum == 0 && new_quote_balance == 0) command_tree
          else throw new AnalyzerParsingException("gobble_commands: Parenthesis or quotes do not match")
        }
      }
    }
    //adding 2 trailing spaces because we always make a check on char(i -2 )
    loop("  ".toList ::: commands_string.toList, 2, List(0), 0, "", "", new ConjunctionOperator(List()))
  }

} //end class


