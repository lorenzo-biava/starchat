package com.getjenny.analyzer.expressions

case class AnalyzersData(
                 traversed_states: List[String] = List.empty[String],
                 extracted_variables: Map[String, String] = Map.empty[String, String]
               )

case class AnalyzersDataInternal(
                          traversed_states: List[String] = List.empty[String],
                          extracted_variables: Map[String, String] = Map.empty[String, String],
                          data: Map[String, Any] = Map.empty[String, Any]
                        )
