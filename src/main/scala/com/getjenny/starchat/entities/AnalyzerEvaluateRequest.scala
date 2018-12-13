package com.getjenny.starchat.entities

/**
  * Created by angelo on 07/04/17.
  */

import com.getjenny.analyzer.expressions.AnalyzersData

case class AnalyzerEvaluateRequest(analyzer: String,
                                   query: String,
                                   data: Option[AnalyzersData],
                                   searchAlgorithm: Option[SearchAlgorithm.Value] = Some(SearchAlgorithm.DEFAULT),
                                   evaluationClass: Option[String] = Some("default")
                                  )
