package homework4.spiders

import homework4.generic.processors.GenericFileOutput
import homework4.math.Monoid
import homework4.processors.{FileOutput, SavedFiles}

class FileOutputSpider(targetDir: String) extends ProcessingSpider:
  type Output = SavedFiles

  def processor = new GenericFileOutput(targetDir)

  def monoid: Monoid[SavedFiles] = summon

  def prettify(output: SavedFiles): String = output.urlToPath
    .map((url, path) => s"$url was saved to $path")
    .mkString("\n")
