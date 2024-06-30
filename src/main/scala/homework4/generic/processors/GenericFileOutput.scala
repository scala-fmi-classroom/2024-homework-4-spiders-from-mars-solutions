package homework4.generic.processors

import homework4.generic.{Concurrent, GenericProcessor}
import homework4.http.HttpResponse
import homework4.processors.SavedFiles

import java.net.URI
import java.nio.file.{Files, Path, Paths}
import java.util.UUID

class GenericFileOutput(targetDir: String) extends GenericProcessor[SavedFiles]:
  private val targetPath = Paths.get(targetDir)

  private def generatePathFor(url: String): Path =
    val urlFileName = Option(Paths.get(new URI(url).getPath).getFileName).map(_.toString).getOrElse("")
    val fileName = s"${UUID.randomUUID().toString}-$urlFileName"

    targetPath.resolve(fileName)

  // Forgot to add "blocking" to Concurrent, so using "pure" here, sorry :)
  def apply[F[_] : Concurrent](url: String, response: HttpResponse): F[SavedFiles] = Concurrent[F].pure:
    if response.isSuccess then
      val path = Files.write(generatePathFor(url), response.bodyAsBytes)

      SavedFiles(Map(url -> path))
    else SavedFiles.Empty
