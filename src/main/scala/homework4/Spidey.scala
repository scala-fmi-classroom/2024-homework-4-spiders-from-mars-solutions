package homework4

import cats.effect.IO
import cats.syntax.all.*
import homework4.html.HtmlUtils
import homework4.http.*
import homework4.math.Monoid

import scala.util.control.NonFatal

case class SpideyConfig(
  maxDepth: Int,
  sameDomainOnly: Boolean = true,
  tolerateErrors: Boolean = true,
  retriesOnError: Int = 0
)

case class CrawlingResult[O](links: List[String], output: O)

class Spidey(httpClient: HttpClient):
  private def linksOf(url: String, response: HttpResponse): List[String] =
    if response.contentType.exists(_.mimeType == ContentType.HtmlMimeType) then
      HtmlUtils
        .linksOf(response.body, url)
        .distinct
    else List.empty

  private def mayVisitUrl(startingUrl: String, sameDomainOnly: Boolean)(url: String): Boolean =
    HttpUtils.isUrlHttp(url) && (!sameDomainOnly || HttpUtils.sameDomain(startingUrl, url))

  private def retry(retries: Int)(urlRetriever: IO[HttpResponse]): IO[HttpResponse] =
    urlRetriever.redeemWith(
      error => if retries > 0 then retry(retries - 1)(urlRetriever) else error.raiseError,
      response => if response.isServerError then retry(retries - 1)(urlRetriever) else response.pure
    )

  def crawl[O : Monoid](startingUrl: String, config: SpideyConfig)(processor: Processor[O]): IO[O] =
    def crawl(depth: Int)(urls: List[String], visitedUrls: Set[String], output: O): IO[O] =
      urls
        .filter(mayVisitUrl(startingUrl, config.sameDomainOnly))
        .filterNot(visitedUrls)
        .parTraverse(crawlSingleUrl)
        .flatMap: results =>
          val currentLevelOutputs = results.map(_.output)
          val combinedOutput = output |+| Monoid.sum(currentLevelOutputs)

          if depth == config.maxDepth then combinedOutput.pure
          else
            crawl(depth + 1)(
              urls = results.flatMap(_.links).distinct,
              visitedUrls = visitedUrls ++ urls,
              combinedOutput
            )

    def crawlSingleUrl(url: String): IO[CrawlingResult[O]] =
      (for
        response <- retry(config.retriesOnError)(httpClient.get(url))
        output <- processor(url, response)
      yield CrawlingResult(linksOf(url, response), output)).recover:
        case NonFatal(_) if config.tolerateErrors => CrawlingResult(List.empty, Monoid[O].identity)

    crawl(0)(List(startingUrl), Set.empty, Monoid[O].identity)
  end crawl
