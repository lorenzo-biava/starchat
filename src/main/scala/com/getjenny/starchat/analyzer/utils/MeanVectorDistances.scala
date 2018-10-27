package com.getjenny.starchat.analyzer.utils

import com.getjenny.analyzer.util.VectorUtils
import com.getjenny.starchat.entities._
import com.getjenny.starchat.services._

/**
  * Created by angelo on 06/09/18.
  */

object MeanVectorDistances {
  val termService: TermService.type = TermService

  def distanceCosine(textTerms1: TextTerms, textTerms2: TextTerms): Double = {
    val text1V = TextToVectorsTools.meanOfTermsVectors(textTerms1)
    val text2V = TextToVectorsTools.meanOfTermsVectors(textTerms2)
    val distance = VectorUtils.cosineDist(text1V._1, text2V._1)
    (1 - distance) * (text1V._2 * text2V._2)
  }
}
