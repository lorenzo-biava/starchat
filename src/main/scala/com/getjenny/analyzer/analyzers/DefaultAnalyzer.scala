package com.getjenny.analyzer.analyzers

/**
  * Created by mal on 20/02/2017.
  */

import com.getjenny.analyzer.operators._
import com.getjenny.analyzer.atoms._

class DefaultAnalyzer(command_string: String)
  extends {
    override val atomicFactory = new DefaultFactoryAtomic
    override val operatorFactory = new DefaultFactoryOperator
  } with DefaultParser(command_string: String)
