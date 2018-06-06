package com.getjenny.starchat.entities

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 20/05/18.
  */

case class SynonymExtractionItem(
                          token: TokenizerResponseItem, /** a token element with position and offset annotation */
                          synonymItem: List[SynonymItem] /** list of suggested synonyms */
                          )