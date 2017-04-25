package com.getjenny.starchat.entities

/**
  * Created by angelo@getjenny.com on 21/04/17.
  */

case class SpellcheckTermsRequest(
  text: String,
  prefix_length: Int = 3,
  min_doc_freq: Int = 1
)

