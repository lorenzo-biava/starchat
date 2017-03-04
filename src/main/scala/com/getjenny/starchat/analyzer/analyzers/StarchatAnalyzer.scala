package com.getjenny.starchat.analyzer.analyzers

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 03/03/17.
  */

import com.getjenny.analyzer.analyzers._
import com.getjenny.analyzer.operators._
import com.getjenny.analyzer.atoms._

class StarchatAnalyzer(command_string: String)
  extends {
    override val atomicFactory = new DefaultFactoryAtomic
    override val operatorFactory = new DefaultFactoryOperator
  } with DefaultParser(command_string: String)
