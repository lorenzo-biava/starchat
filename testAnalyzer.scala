import com.getjenny.starchat.analyzer._

object TestAnalyzer {

  val k = new Keyword("ciao")
  assert(k.evaluate("ciao, stupid moron") == 1.0/3)
  assert(k.matches("ciao stupid moron"))

  val anal = new DefaultAnalyzer("""and ( keyword ("ciao"), or ( keyword ("stupid"), keyword ("idiot")  )  )""")
  assert(anal.evaluate("ciao stupid moron") == 1)
  assert(anal.evaluate("ciao moron idiot") == 1)
  assert(anal.evaluate("stupid  idiot") == 0)

  // either "stupido" or "idiota"
  val analBayes = new DefaultAnalyzer("""disjunction( keyword("stupid"), keyword("idiot") )""")

  val idiot = analBayes.evaluate("ciao nice idiot moron")
  val stupid_idiot_long = analBayes.evaluate("ciao stupid moron idiot")
  val stupid_idiot_short = analBayes.evaluate("ciao stupid idiot")

  //two is better than one
  assert(stupid_idiot_long > idiot)
  assert(stupid_idiot_short > idiot)

  //finding in short is better than finding in longer
  assert(stupid_idiot_short > stupid_idiot_long)

}