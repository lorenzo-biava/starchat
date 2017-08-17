/**
  * Created by Angelo Leto <angelo@getjenny.com> on 03/03/17.
  */

import com.getjenny.analyzer.analyzers._
import org.scalatest._

class AnalyzersTest extends FlatSpec with Matchers {

  "A DefaultAnalyzer" should "parse a rule and evaluate the operations on a provided input text" in {
    val analyzerBayes = new DefaultAnalyzer("""disjunction( keyword("clever"), keyword("gentleman") )""")

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
      new DefaultAnalyzer("""disjunction( keyword("clever")), keyword("gentleman") )""")
    }
  }
  /*
  //TODO: enable after changing the parser
  it should "throw a AnalyzerCommandException if the command is not supported" in {
    a [AnalyzerCommandException] should be thrownBy {
      new DefaultAnalyzer("""fakeDisjunction( keyword("clever"), keyword("gentleman") )""")
    }
  }*/

}
