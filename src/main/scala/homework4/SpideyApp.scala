package homework4

import cats.effect.{ExitCode, IO, IOApp}
import homework4.http.AsyncHttpClient
import homework4.spiders.{BrokenLinkDetectorSpider, FileOutputSpider, ProcessingSpider, WordCounterSpider}

import scala.util.Try

case class SpideyAppInput(url: String, maxDepth: Int, spider: ProcessingSpider)

import cats.syntax.all.*

object NonNegativeInteger:
  def unapply(integerString: String): Option[Int] =
    Try(integerString.toInt).toOption.filter(_ >= 0)

object SpideyApp extends IOApp:
  val defaultConfig = SpideyConfig(
    maxDepth = 0,
    sameDomainOnly = false,
    tolerateErrors = true,
    retriesOnError = 0
  )

  val usage: String =
    """
      |Usage:
      |
      |SpideyApp <url> <max-depth> <processor> [processor-config]
      |
      |Possible processors and their config are:
      |
      |file-output <target-dir>
      |word-counter
      |broken-link-detector
      """.stripMargin

  def parseArguments(args: List[String]): Option[SpideyAppInput] =
    def chooseSpider(processor: String, processorArgs: Seq[String]): Option[ProcessingSpider] = processor match
      case "file-output" =>
        processorArgs match
          case Seq(targetDir) => Some(new FileOutputSpider(targetDir))
          case _ => None
      case "word-counter" => Some(WordCounterSpider)
      case "broken-link-detector" => Some(BrokenLinkDetectorSpider)
      case _ => None

    args match
      case List(url, NonNegativeInteger(maxDepth), processor, processorArgs*) =>
        chooseSpider(processor, processorArgs)
          .map(SpideyAppInput(url, maxDepth, _))
      case _ => None

  def runSpider(url: String, maxDepth: Int, spider: ProcessingSpider): IO[String] =
    val httpClient = new AsyncHttpClient
    val spidey = new Spidey(httpClient)

    spider
      .process(spidey, url, maxDepth)(defaultConfig)
      .guarantee(httpClient.shutdown())

  def run(args: List[String]): IO[ExitCode] =
    parseArguments(args) match
      case Some(SpideyAppInput(url, maxDepth, spider)) =>
        val spiderApp =
          runSpider(url, maxDepth, spider)
            >>= { output => IO.println(s"Spidey retrieved the following results:\n\n$output") }

        spiderApp.as(ExitCode.Success)
      case None =>
        (IO.println("Invalid arguments") >> IO.println(usage)).as(ExitCode.Error)

  val httpClient = new AsyncHttpClient
  val spidey = new Spidey(httpClient)
