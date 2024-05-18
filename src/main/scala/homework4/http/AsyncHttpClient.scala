package homework4.http

import cats.effect.IO
import cats.effect.kernel.Resource
import org.asynchttpclient.Dsl.asyncHttpClient
import org.asynchttpclient.{ListenableFuture, Response}

import java.util.concurrent.ExecutionException
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Try}

class AsyncHttpClient extends HttpClient:
  private val client = asyncHttpClient()

  def get(url: String): IO[HttpResponse] =
    IO.executionContext.flatMap: ec =>
      cats.effect.IO.async_ : callback =>
        val eventualResponse = client.prepareGet(url).setFollowRedirect(true).execute()
        eventualResponse.addListener(() => callback(handleResponse(eventualResponse).toEither), r => ec.execute(r))

  private def toHttpResponse(response: Response): HttpResponse = new HttpResponse:
    def status: Int = response.getStatusCode

    def headers: Map[String, String] = response.getHeaders.asScala.map(es => (es.getKey.toLowerCase, es.getValue)).toMap

    def bodyAsBytes: Array[Byte] = response.getResponseBodyAsBytes

  private def handleResponse(eventualResponse: ListenableFuture[Response]): Try[HttpResponse] =
    Try:
      toHttpResponse(eventualResponse.get())
    .recoverWith:
      case e: ExecutionException => Failure(e.getCause)

  def shutdown(): IO[Unit] = IO.blocking(client.close())

object AsyncHttpClient:
  def asResource: Resource[IO, AsyncHttpClient] =
    Resource.make(IO.pure(AsyncHttpClient()))(client => IO.blocking(client.shutdown()))
