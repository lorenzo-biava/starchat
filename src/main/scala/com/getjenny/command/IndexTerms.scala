package io.elegans.command

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, _}
import scopt.OptionParser

import scala.concurrent.Future

object CalcTermFreq {

//  lazy val textProcessingUtils = new TextProcessingUtils /* lazy initialization of TextProcessingUtils class */
//  lazy val loadData = new LoadData

  private case class Params(
    host: Option[String] = "http://localhost",
    port: Int = 8888,
    path: Option[String] = "/"
    numpartitions: Int = 1000,
    appname: String = "CalcTerm freq",
    minfreq: Int = 0
  )

  private def doIndexTerms(params: Params) {
    val responseFuture: Future[HttpResponse] =
      Http().singleRequest(HttpRequest(uri = "http://akka.io"))
  }

  def main(args: Array[String]) {
    val defaultParams = Params()
    val parser = new OptionParser[Params]("Tokenize terms and count term frequency from a set of files") {
      head("calculate term frequency.")
      help("help").text("prints this usage text")
      opt[String]("inputfile")
        .text(s"the path e.g. tmp/dir" +
          s"  default: ${defaultParams.inputfile}")
        .action((x, c) => c.copy(inputfile = Option(x)))
      opt[String]("appname")
        .text(s"application name" +
          s"  default: ${defaultParams.appname}")
        .action((x, c) => c.copy(appname = x))
      opt[String]("outputDir")
        .text(s"the where to store the output files: topics and document per topics" +
          s"  default: ${defaultParams.outputDir}")
        .action((x, c) => c.copy(outputDir = x))
      opt[Int]("levels")
        .text(s"the levels where to search for files e.g. for level=2 => tmp/dir/*/*" +
          s"  default: ${defaultParams.levels}")
        .action((x, c) => c.copy(levels = x))
      opt[Int]("numpartitions")
        .text(s"the number of partitions reading files" +
          s"  default: ${defaultParams.numpartitions}")
        .action((x, c) => c.copy(numpartitions = x))
      opt[Int]("minfreq")
        .text(s"remove words with a frequency less that minfreq" +
          s"  default: ${defaultParams.minfreq}")
        .action((x, c) => c.copy(minfreq = x))
    }

    parser.parse(args, defaultParams) match {
      case Some(params) =>
        doIndexTerms(params)
      case _ =>
        sys.exit(1)
    }
  }
}
