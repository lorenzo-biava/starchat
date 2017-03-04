import com.getjenny.starchat.analyzer.analyzers._
import com.getjenny.starchat.analyzer.atoms._
import org.scalatest._

class AnalyzersTest extends FlatSpec with Matchers {

  "An AtomicKeyword" should "support a floating point or a boolean value" in {
    val k = new AtomicKeyword("ciao")
    k.evaluate("ciao, stupid moron") should be (1.0/3)
    k.matches("ciao stupid moron") should be (true)
  }

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