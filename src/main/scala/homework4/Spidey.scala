package homework4

import cats.effect.IO
import cats.syntax.all.*
import homework4.http.*
import homework4.math.Monoid

import cats.syntax.all.*

case class SpideyConfig(
  maxDepth: Int,
  sameDomainOnly: Boolean = true,
  tolerateErrors: Boolean = true,
  retriesOnError: Int = 0
)

class Spidey(httpClient: HttpClient):
  def crawl[O : Monoid](url: String, config: SpideyConfig)(processor: Processor[O]): IO[O] = ???
