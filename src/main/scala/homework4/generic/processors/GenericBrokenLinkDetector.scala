package homework4.generic.processors

import homework4.generic.{Concurrent, GenericProcessor}
import homework4.http.HttpResponse

object GenericBrokenLinkDetector extends GenericProcessor[Set[String]]:
  def apply[F[_] : Concurrent](url: String, response: HttpResponse): F[Set[String]] = Concurrent[F].pure:
    if response.isSuccess then Set(url)
    else Set.empty
