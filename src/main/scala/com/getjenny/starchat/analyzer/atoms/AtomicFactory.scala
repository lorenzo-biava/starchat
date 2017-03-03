package com.getjenny.starchat.analyzer.atoms

/**
  * Created by mal on 20/02/2017.
  */
object AtomicFactory {

  val atoms = Set("keyword" , "similar", "synonym", "regex")

  def returnAtoms(name: String, argument: String): AbstractAtomic = name.filter(c => !c.isWhitespace ) match {

    case "keyword" => new AtomicKeyword(argument)
    case "similar" => new AtomicW2VCosineSentence(argument)
    case "synonym" => new AtomicW2VCosineWord(argument)
    case "regex" => new AtomicRegularExpression(argument)
    case _ => throw new Exception("Atom \'" + name + "\' not found")

  }

}
