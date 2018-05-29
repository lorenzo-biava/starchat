package com.getjenny.starchat.entities

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 20/05/18.
  */

case class SynExtractionRequest (
                text: String,
                commonOrSpecificSearchTerms: Option[CommonOrSpecificSearch.Value] = Some(CommonOrSpecificSearch.COMMON),
                commonOrSpecificSearchPrior: Option[CommonOrSpecificSearch.Value] = Some(CommonOrSpecificSearch.COMMON),
                commonOrSpecificSearchObserved: Option[CommonOrSpecificSearch.Value] = Some(CommonOrSpecificSearch.IDXSPECIFIC),
                obsDest: Option[ObservedSearchDests.Value] = Some(ObservedSearchDests.KNOWLEDGEBASE),
                termFieldPrior: Option[TermCountFields.Value] = Some(TermCountFields.question),
                termFieldObserved: Option[TermCountFields.Value] = Some(TermCountFields.question),
                minWordsPerSentence: Option[Long] = Some(10),
                pruneTermsThreshold: Option[Int] = Some(100000),
                misspell_max_occurrence: Option[Int] = Some(5),
                active_potential_decay: Option[Int] = Some(10),
                active_potential: Option[Boolean] = Some(true),
                total_info: Option[Boolean] = Some(false)
                )
