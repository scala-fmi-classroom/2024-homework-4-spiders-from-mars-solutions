package homework4.processors

import cats.effect.IO
import homework4.Processor
import homework4.http.HttpResponse
import homework4.math.Monoid

import java.net.URI
import java.nio.file.{Path, Paths}
import java.util.UUID

import cats.syntax.all.*

case class SavedFiles(urlToPath: Map[String, Path])

object SavedFiles:
  given Monoid[SavedFiles] = ???

class FileOutput(targetDir: String) extends Processor[SavedFiles]:
  private val targetPath = Paths.get(targetDir)

  private def generatePathFor(url: String): Path =
    val urlFileName = Option(Paths.get(new URI(url).getPath).getFileName).map(_.toString).getOrElse("")
    val fileName = s"${UUID.randomUUID().toString}-$urlFileName"

    targetPath.resolve(fileName)

  def apply(url: String, response: HttpResponse): IO[SavedFiles] = ???
