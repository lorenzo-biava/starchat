package com.getjenny.analyzer.expressions

case class Data(
                 item_list: List[String] = List.empty[String],
                 extracted_variables: Map[String, String] = Map.empty[String, String]
               )
