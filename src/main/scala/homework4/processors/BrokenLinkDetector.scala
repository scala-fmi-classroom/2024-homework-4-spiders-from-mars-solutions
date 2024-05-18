package homework4.processors

import cats.effect.IO
import homework4.Processor
import homework4.http.HttpResponse

import cats.syntax.all.*

object BrokenLinkDetector extends Processor[Set[String]]:
  def apply(url: String, response: HttpResponse): IO[Set[String]] = ???
