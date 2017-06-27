package com.getjenny.starchat.entities

/**
  * Created by angelo on 07/04/17.
  */

case class AnalyzerEvaluateResponse(build: Boolean, value: Double, variables: Map[String, String],
                                    build_message: String)
