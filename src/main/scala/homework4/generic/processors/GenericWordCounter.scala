package homework4.generic.processors

import homework4.generic.{Concurrent, GenericProcessor}
import homework4.html.HtmlUtils
import homework4.http.{ContentType, HttpResponse}
import homework4.processors.WordCount

object GenericWordCounter extends GenericProcessor[WordCount]:
  def asText(response: HttpResponse): Option[String] =
    response.contentType
      .map(_.mimeType)
      .collect:
        case ContentType.HtmlMimeType => HtmlUtils.toText(response.body)
        case ContentType.PlainTextMimeType => response.body

  def countWords(text: String): WordCount =
    WordCount(
      WordCount
        .wordsOf(text)
        .groupBy(identity)
        .view
        .mapValues(_.size)
        .toMap
    )

  def apply[F[_] : Concurrent](url: String, response: HttpResponse): F[WordCount] = Concurrent[F].pure:
    if response.isSuccess then asText(response).map(countWords).getOrElse(WordCount.Empty)
    else WordCount.Empty
