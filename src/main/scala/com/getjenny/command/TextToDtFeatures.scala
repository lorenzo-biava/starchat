package com.getjenny.command

/**
  * Created by angelo on 05/09/18.
  */

import java.io._

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.getjenny.starchat.serializers.JsonSupport
import edu.stanford.nlp.process.DocumentPreprocessor
import scopt.OptionParser

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.ExecutionContextExecutor

object TextToDtFeatures extends JsonSupport {

  private[this] case class Params(
                                   inputfile: String = "file.txt",
                                   out_dictionary: String = "dictionary.tsv",
                                   out_training: String = "training.data",
                                   out_names: String = "features.names",
                                   window: Int = 5,
                                   center: Int= 2,

                                 )

  private[this] def execute(params: Params) {
    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    val reader: FileReader = new FileReader(params.inputfile)
    val docProcecessor: DocumentPreprocessor = new DocumentPreprocessor(reader)
    val sentencesIt = docProcecessor.iterator.asScala
    val tokenizedSentences = sentencesIt.map { case (sentence) =>
      sentence.asScala.map { case(token) => token.word.toLowerCase }.toVector
        .filter(! _.matches("([^\\p{L}|^\\d])")) // filtering strings with just one punctuation token
    }

    val nullFeature: Long = 0
    val dict = mutable.Map[String, Long]("" -> nullFeature) // empty feature
    val sentenceFeatures = tokenizedSentences.map { case(sentence) =>
        sentence.map { case (token) =>
            dict.get(token) match {
              case Some(i) => i
              case _ =>
                val newIndex = dict.size.toLong
                dict(token) = newIndex
                newIndex
            }
        }
    }.filter(_.length > 1)

    val dictionaryWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(params.out_dictionary)))
    dict.foreach { case(key, value) =>
      dictionaryWriter.write(key + "\t" + value)
      dictionaryWriter.newLine
    }
    dictionaryWriter.close()

    val window = params.window
    val center = params.center
    val dictSize = dict.size

    val names: List[String] = List("expansions.\t\t\t\t| the target attribute") ++
      List.range(0, window).map { case (e) =>
        "e" + e + ":\t\t\t\t" + List.range(0, dictSize).mkString(",") + "."
      } ++ List("ID:\t\t\t\tlabel.")

    val namesWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(params.out_names)))
    names.foreach { case(line) =>
      namesWriter.write(line)
      namesWriter.newLine()
    }
    namesWriter.close()

    val leftPad = center
    val rightPad = window - center + 1
    val features = sentenceFeatures.map{ case (sentence) =>
      val paddedSentence = Vector.fill(leftPad)(nullFeature) ++ sentence ++ Vector.fill(rightPad)(nullFeature)
      paddedSentence.sliding(window).filter(v => v(center) != nullFeature)
    }.flatMap { case(vectors) =>
      vectors.map { case (vector) =>
        val centerValue = vector(center)
        vector.patch(center, List[Long](), 1) ++ Vector[Long](centerValue)
      }
    }

    val featuresWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(params.out_dictionary)))
    features.zipWithIndex.map{ case(v) => v._1 ++ Vector(v._2.toLong)}.foreach { case(row) =>
      val entry = row.mkString(",")
      featuresWriter.write(entry)
      featuresWriter.newLine()
    }
    featuresWriter.close()

  }

  def main(args: Array[String]) {
    val defaultParams = Params()
    val parser = new OptionParser[Params]("IndexSubtitles") {
      head("Index data into stat_text")
      help("help").text("prints this usage text")
      opt[String]("inputfile")
        .text(s"the path of the file with the subtitles" +
          s"  default: ${defaultParams.inputfile}")
        .action((x, c) => c.copy(inputfile = x))
      opt[String]("out_dictionary")
        .text(s"the output file where to save the dictionary" +
          s"  default: ${defaultParams.out_dictionary}")
        .action((x, c) => c.copy(out_dictionary = x))
      opt[String]("out_training")
        .text(s"the output file where to save the training data" +
          s"  default: ${defaultParams.out_training}")
        .action((x, c) => c.copy(out_training = x))
      opt[String]("out_names")
        .text(s"the output file where to save the features names" +
          s"  default: ${defaultParams.out_names}")
        .action((x, c) => c.copy(out_names = x))
      opt[Int]("window")
        .text(s"the size of the sliding window " +
          s"  default: ${defaultParams.window}")
        .action((x, c) => c.copy(window = x))
      opt[Int]("center")
        .text(s"the index of the center of the window starting from 0" +
          s"  default: ${defaultParams.center}")
        .action((x, c) => c.copy(center = x))
    }

    parser.parse(args, defaultParams) match {
      case Some(params) =>
        execute(params)
        sys.exit(0)
      case _ =>
        sys.exit(1)
    }
  }
}
