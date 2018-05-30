package com.getjenny.starchat.entities

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 20/05/18.
  */

case class TermsExtractionRequest (
                                    text: String,
                                    commonOrSpecificSearchPrior: Option[CommonOrSpecificSearch.Value] = Some(CommonOrSpecificSearch.COMMON),
                                    commonOrSpecificSearchObserved: Option[CommonOrSpecificSearch.Value] = Some(CommonOrSpecificSearch.IDXSPECIFIC),
                                    obsDest: Option[ObservedSearchDests.Value] = Some(ObservedSearchDests.KNOWLEDGEBASE),
                                    fieldsPrior: Option[TermCountFields.Value] = Some(TermCountFields.question),
                                    fieldsObserved: Option[TermCountFields.Value] = Some(TermCountFields.question),
                                    minWordsPerSentence: Option[Int] = Some(10),
                                    pruneTermsThreshold: Option[Int] = Some(100000),
                                    misspellMaxOccurrence: Option[Int] = Some(5),
                                    activePotentialDecay: Option[Int] = Some(10),
                                    activePotential: Option[Boolean] = Some(true),
                                    totalInfo: Option[Boolean] = Some(false)
                                  )
