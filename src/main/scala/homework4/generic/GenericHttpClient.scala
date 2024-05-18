package homework4.generic

import homework4.http.HttpResponse

trait GenericHttpClient:
  def get[F[_] : Concurrent](url: String): F[HttpResponse]
