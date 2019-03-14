package com.getjenny.starchat.entities

/**
  * Created by Angelo Leto <angelo@getjenny.com> on 14/03/19.
  */

import scalaz.Scalaz._

object QAAggregationsTypes extends Enumeration {
  val totalConversations, // the total number of conversations (transferred and non-transferred)
  avgFeedbackConvScore, // the average score for the conversations (transferred and non-transferred)
  avgFeedbackAnswerScore, // the average score for all the Answers (transferred and non-transferred)
  avgAlgorithmConvScore, // the average score for the conversations (transferred and non-transferred), the scores are provided by an algorithm
  avgAlgorithmAnswerScore, // the average score for all the Answers (transferred and non-transferred), the scores are provided by an algorithm
  scoreHistogram, // the distribution of scores, an histogram (transferred and non-transferred)
  scoreHistogramNonTransferred, // the distribution of scores for the non-transferred conversations, an histogram
  scoreHistogramTransferred, // the distribution of scores for the transferred conversations, an histogram
  conversationsHistogram, // time range histogram of the number of conversations on a time range (transferred and non-transferred)
  conversationsNonTransferredHistogram, // time range histogram of the number of conversations on a time range (non-transferred only)
  conversationsTransferredHistogram, // time range histogram of the number of conversations on a time range (transferred only)
  avgFeedbackNonTransferredConvScoreOverTime, // average score for the conversations (non-transferred only) on each time interval
  avgFeedbackTransferredConvScoreOverTime, // average score for the conversations (transferred only) on each time interval
  avgAlgorithmNonTransferredConvScoreOverTime, // average score for the conversations (non-transferred only) on each time interval, the scores are provided by an algorithm
  avgAlgorithmTransferredConvScoreOverTime, // average score for the conversations (transferred only) on each time interval, the scores are provided by an algorithm
  avgFeedbackNonTransferredAnswerScoreOverTime, // average score for the answers (non-transferred only) on each time interval
  avgFeedbackTransferredAnswerScoreOverTime, // average score for the answers (transferred only) on each time interval
  avgAlgorithmNonTransferredAnswerScoreOverTime, // average score for the answers (non-transferred only) on each time interval, the scores are provided by an algorithm
  avgAlgorithmTransferredAnswerScoreOverTime, // average score for the answers (transferred only) on each time interval, the scores are provided by an algorithm
  avgFeedbackConvScoreOverTime, // average score for the conversations (transferred and non-transferred) on each time interval
  avgAlgorithmAnswerScoreOverTime, // average score for the answers (transferred and transferred) on each time interval
  avgFeedbackAnswerScoreOverTime, // average score for the answers (transferred and non-transferred) on each time interval, the scores are provided by an algorithm
  avgAlgorithmConvScoreOverTime, // average score for the answers (transferred and transferred) on each time interval, the scores are provided by an algorithm
  UNKNOWN = QAAggregationsTypes.Value  /** normal document, can be returned to the user as response */
  def value(v: String): QAAggregationsTypes.Value = values.find(_.toString === v).getOrElse(UNKNOWN)
}
