package homework4.generic

import homework4.html.HtmlUtils
import homework4.http.*
import homework4.math.Monoid
import homework4.{CrawlingResult, SpideyConfig}

import scala.util.control.NonFatal
import scala.util.{Failure, Success}

class GenericSpidey[F[_] : Concurrent](httpClient: GenericHttpClient):
  private def linksOf(url: String, response: HttpResponse): List[String] =
    if response.contentType.exists(_.mimeType == ContentType.HtmlMimeType) then
      HtmlUtils
        .linksOf(response.body, url)
        .distinct
    else List.empty

  private def mayVisitUrl(startingUrl: String, sameDomainOnly: Boolean)(url: String): Boolean =
    HttpUtils.isUrlHttp(url) && (!sameDomainOnly || HttpUtils.sameDomain(startingUrl, url))

  private def retry(retries: Int)(urlRetriever: => F[HttpResponse]): F[HttpResponse] =
    urlRetriever.transformWith[HttpResponse]:
      case Success(response) if !response.isServerError => Concurrent[F].pure(response)
      case Success(_) | Failure(NonFatal(_)) if retries > 0 => retry(retries - 1)(urlRetriever)
      case result => Concurrent[F].fromTry(result)

  def crawl[O : Monoid](startingUrl: String, config: SpideyConfig)(processor: GenericProcessor[O]): F[O] =
    def crawl(depth: Int)(urls: List[String], visitedUrls: Set[String], output: O): F[O] =
      urls
        .filter(mayVisitUrl(startingUrl, config.sameDomainOnly))
        .filterNot(visitedUrls)
        .parTraverse(crawlSingleUrl)
        .flatMap: results =>
          val currentLevelOutputs = results.map(_.output)
          val combinedOutput = output |+| Monoid.sum(currentLevelOutputs)

          if depth == config.maxDepth then Concurrent[F].pure(combinedOutput)
          else
            crawl(depth + 1)(
              urls = results.flatMap(_.links).distinct,
              visitedUrls = visitedUrls ++ urls,
              combinedOutput
            )

    def crawlSingleUrl(url: String): F[CrawlingResult[O]] =
      (for
        response <- retry(config.retriesOnError)(httpClient.get(url))
        output <- processor(url, response)
      yield CrawlingResult(linksOf(url, response), output)).recover:
        case NonFatal(_) if config.tolerateErrors => CrawlingResult(List.empty, Monoid[O].identity)

    crawl(0)(List(startingUrl), Set.empty, Monoid[O].identity)
  end crawl
