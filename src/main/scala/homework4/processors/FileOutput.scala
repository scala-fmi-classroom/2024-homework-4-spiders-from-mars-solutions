package homework4.processors

import cats.effect.IO
import cats.syntax.all.*
import homework4.Processor
import homework4.http.HttpResponse
import homework4.math.Monoid

import java.net.URI
import java.nio.file.{Files, Path, Paths}
import java.util.UUID

case class SavedFiles(urlToPath: Map[String, Path])

object SavedFiles:
  def Empty: SavedFiles = SavedFiles(Map.empty)

  given Monoid[SavedFiles] with
    extension (a: SavedFiles) def |+|(b: SavedFiles): SavedFiles = SavedFiles(a.urlToPath ++ b.urlToPath)

    def identity: SavedFiles = Empty

class FileOutput(targetDir: String) extends Processor[SavedFiles]:
  private val targetPath = Paths.get(targetDir)

  private def generatePathFor(url: String): Path =
    val urlFileName = Option(Paths.get(new URI(url).getPath).getFileName).map(_.toString).getOrElse("")
    val fileName = s"${UUID.randomUUID().toString}-$urlFileName"

    targetPath.resolve(fileName)

  def apply(url: String, response: HttpResponse): IO[SavedFiles] = IO.blocking:
    if response.isSuccess then
      val path = Files.write(generatePathFor(url), response.bodyAsBytes)

      SavedFiles(Map(url -> path))
    else SavedFiles.Empty
