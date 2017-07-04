package com.getjenny.starchat.entities

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 10/02/17.
  */

import scala.collection.mutable.LinkedHashMap

case class DTAnalyzerLoad(num_of_entries: Int)

case class DTAnalyzerItem(analyzer: String, build: Boolean, execution_order: Int)

case class DTAnalyzerMap(analyzer_map: Map[String, DTAnalyzerItem])
