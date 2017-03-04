package com.getjenny.starchat.analyzer.atoms

import com.getjenny.starchat.util.Vectors._
import com.getjenny.starchat.analyzer.expressions._

/**
  * Created by mal on 20/02/2017.
  */

/**
  * These are the Atomic components of Analyzer. Atomic can be for instance a Pattern (i.e. a Regex)
  * or a Similarity.
  * Atomics can be composed in Analyzers, like in and(regex > 0, similarity > 0.9)
  */
abstract class AbstractAtomic extends Expression {
  val isEvaluateNormalized: Boolean  // does "evaluate" return normalized values?
}
