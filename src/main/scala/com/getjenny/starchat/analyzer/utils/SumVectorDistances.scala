package com.getjenny.starchat.analyzer.utils

import com.getjenny.analyzer.util.VectorUtils
import com.getjenny.starchat.entities._
import com.getjenny.starchat.services._

/**
  * Created by angelo on 04/04/17.
  */

object SumVectorDistances {
  val termService: TermService.type = TermService

  def distanceCosine(textTerms1: TextTerms, textTerms2: TextTerms): Double = {
    val text1V = TextToVectorsTools.sumOfTermsVectors(textTerms1)
    val text2V = TextToVectorsTools.sumOfTermsVectors(textTerms2)
    val distance = VectorUtils.cosineDist(text1V._1, text2V._1)
    (1 - distance) * (text1V._2 * text2V._2)
  }
}
