package homework4.generic.processors

import homework4.generic.{Concurrent, GenericProcessor}
import homework4.http.HttpResponse
import homework4.processors.SavedFiles

class GenericFileOutput(targetDir: String) extends GenericProcessor[SavedFiles]:
  def apply[F[_] : Concurrent](url: String, response: HttpResponse): F[SavedFiles] = ???
