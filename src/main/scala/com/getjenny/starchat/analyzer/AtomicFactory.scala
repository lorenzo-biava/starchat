package com.getjenny.starchat.analyzer

/**
  * Created by mal on 20/02/2017.
  */
object AtomicFactory {

  val atoms = Set("keyword" , "similar", "synonym", "regex")

  def returnAtoms(name: String, argument: String): Atomic = name.filter(c => !c.isWhitespace ) match {

    case "keyword" => new Keyword(argument)
    case "similar" => new W2VCosineSentence(argument)
    case "synonym" => new W2VCosineWord(argument)
    case "regex" => new RegularExpression(argument)
    case _ => throw new Exception("Atom \'" + name + "\' not found")

  }

}
