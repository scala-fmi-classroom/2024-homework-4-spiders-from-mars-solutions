package homework4.http

import cats.effect.IO

trait HttpClient:
  def get(url: String): IO[HttpResponse]
