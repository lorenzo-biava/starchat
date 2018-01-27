package com.getjenny.analyzer.expressions

case class AnalyzersData(
                          itemList: List[String] = List.empty[String],
                          extractedVariables: Map[String, String] = Map.empty[String, String],
                          data: Map[String, Any] = Map.empty[String, Any]
                        )

case class Data(
                 itemList: List[String] = List.empty[String],
                 extractedVariables: Map[String, String] = Map.empty[String, String]
               )

