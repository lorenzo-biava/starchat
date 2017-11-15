/**
  * Created by Angelo Leto <angelo@getjenny.com> on 03/03/17.
  */

import com.getjenny.analyzer.analyzers._
import org.scalatest._
import com.getjenny.analyzer.operators.OperatorException

class AnalyzersTest extends FlatSpec with Matchers {

  val restricted_args = Map.empty[String, String]
  "A DefaultAnalyzer" should "parse a rule and evaluate the operations on a provided input text" in {
    val analyzerBayes = new DefaultAnalyzer("""disjunction( keyword("clever"), keyword("gentleman") )""", restricted_args)

    val gentleman = analyzerBayes.evaluate("ciao nice gentleman fool")
    val clever_gentleman_long = analyzerBayes.evaluate("ciao clever fool gentleman")
    val clever_gentleman_short = analyzerBayes.evaluate("ciao clever gentleman")

    //two is better than one
    clever_gentleman_long.score should be > gentleman.score
    clever_gentleman_short.score should be > gentleman.score

    //finding in short is better than finding in longer
    clever_gentleman_short.score should be > clever_gentleman_long.score
  }
  it should "throw a AnalyzerParsingException if parenthesis are not balanced" in {
    a [AnalyzerParsingException] should be thrownBy {
      new DefaultAnalyzer("""disjunction( keyword("clever")), keyword("gentleman") )""", restricted_args)
    }
  }
  it should "throw a AnalyzerCommandException if the command does not exists or is mispelled" in {
    a [AnalyzerCommandException] should be thrownBy {
      new DefaultAnalyzer("""fakeDisjunction( keyword("clever"), keyword("gentleman") )""", restricted_args)
    }
  }

}
