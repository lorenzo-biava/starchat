package com.getjenny.starchat.entities

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 02/07/16.
  */

/*parameters*/
case class RootAPIsDescription(api : Map[String, String] = Map(
  "/" -> "Welcome! For help, see https://github.com/GetJenny/starchat",
  "/get_next_response" -> "supported methods: POST",
  "/decisiontable" -> "supported methods: GET, PUT, POST, DELETE",
  "/decisiontable_search" -> "supported methods: POST",
  "/decisiontable_analyzer" -> "supported methods: GET, POST",
  "/knowledgebase" -> "supported methods: GET, PUT, POST, DELETE",
  "/knowledgebase_search" -> "supported methods: POST",
  "/index_management" -> "supported methods: GET, PUT, POST, DELETE",
  "/language_guesser" -> "supported methods: GET, POST",
  "/analyzers" -> "supported methods: GET, POST"
))
