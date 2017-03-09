package com.getjenny.starchat.analyzer.atoms

/**
  * Created by mal on 20/02/2017.
  */

import com.getjenny.analyzer.interfaces._
import com.getjenny.analyzer.atoms._

class StarchatFactoryAtomic extends Factory[String, AbstractAtomic] {

  override val operations = Set("keyword" , "similar", "synonym", "regex", "search")

  override def get(name: String, argument: String): AbstractAtomic = name.filter(c => !c.isWhitespace ) match {
    case "search" => new SearchAtomic(argument)
    case "keyword" => new KeywordAtomic(argument)
    case "similar" => new W2VCosineSentenceAtomic(argument)
    case "synonym" => new W2VCosineWordAtomic(argument)
    case "regex" => new RegularExpressionAtomic(argument)
    case _ => throw ExceptionAtomic("Atom \'" + name + "\' not found")
  }

}