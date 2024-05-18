package homework4

import cats.effect.IO
import homework4.http.HttpResponse

trait Processor[O]:
  def apply(url: String, response: HttpResponse): IO[O]
