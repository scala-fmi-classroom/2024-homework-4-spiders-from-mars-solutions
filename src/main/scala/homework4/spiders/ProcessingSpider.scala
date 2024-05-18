package homework4.spiders

import cats.effect.IO
import homework4.math.Monoid
import homework4.{Processor, Spidey, SpideyConfig}

trait ProcessingSpider:
  type Output

  def processor: Processor[Output]

  def monoid: Monoid[Output]

  def prettify(output: Output): String

  def process(
    spidey: Spidey,
    url: String,
    maxDepth: Int
  )(
    defaultConfig: SpideyConfig
  ): IO[String] =
    spidey.crawl(url, defaultConfig.copy(maxDepth = maxDepth))(processor)(using monoid).map(prettify)
