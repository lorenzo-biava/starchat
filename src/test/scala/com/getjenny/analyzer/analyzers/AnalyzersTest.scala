/**
  * Created by Angelo Leto <angelo@getjenny.com> on 03/03/17.
  */

import com.getjenny.analyzer.analyzers._
import com.getjenny.analyzer.atoms._
import org.scalatest._

class AnalyzersTest extends FlatSpec with Matchers {

  "A DefaultAnalyzer" should "parse a rule and evaluate the operations on a provided input text" in {
    val analBayes = new DefaultAnalyzer("""disjunction( keyword("stupid"), keyword("idiot") )""")

    val idiot = analBayes.evaluate("ciao nice idiot moron")
    val stupid_idiot_long = analBayes.evaluate("ciao stupid moron idiot")
    val stupid_idiot_short = analBayes.evaluate("ciao stupid idiot")

    //two is better than one
    stupid_idiot_long should be > idiot
    stupid_idiot_short should be > idiot

    //finding in short is better than finding in longer
    stupid_idiot_short should be > stupid_idiot_long
  }
  it should "throw a AnalyzerParsingException if parenthesis are not balanced" in {
    a [AnalyzerParsingException] should be thrownBy {
      new DefaultAnalyzer("""disjunction( keyword("stupid")), keyword("idiot") )""")
    }
  }
  /*
  //TODO: enable after changing the parser
  it should "throw a AnalyzerCommandException if the command is not supported" in {
    a [AnalyzerCommandException] should be thrownBy {
      new DefaultAnalyzer("""fakeDisjunction( keyword("stupid"), keyword("idiot") )""")
    }
  }*/

}