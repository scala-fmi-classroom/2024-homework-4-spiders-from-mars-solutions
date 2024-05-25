package homework4.generic

import homework4.http.{AsyncHttpClient, HttpResponse}
import org.asynchttpclient.Dsl.asyncHttpClient
import org.asynchttpclient.{ListenableFuture, Response}

import java.util.concurrent.ExecutionException
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Try}

class GenericAsyncHttpClient extends GenericHttpClient:
  private val client = asyncHttpClient()

  def get[F[_] : Concurrent](url: String): F[HttpResponse] =
    Concurrent[F].async: (callback, ec) =>
      val eventualResponse = client.prepareGet(url).setFollowRedirect(true).execute()
      eventualResponse.addListener(() => callback(handleResponse(eventualResponse)), r => ec.execute(r))

  private def toHttpResponse(response: Response): HttpResponse = new HttpResponse:
    def status: Int = response.getStatusCode

    def headers: Map[String, String] = response.getHeaders.asScala.map(es => (es.getKey.toLowerCase, es.getValue)).toMap

    def bodyAsBytes: Array[Byte] = response.getResponseBodyAsBytes

  private def handleResponse(eventualResponse: ListenableFuture[Response]): Try[HttpResponse] =
    Try:
      toHttpResponse(eventualResponse.get())
    .recoverWith:
      case e: ExecutionException => Failure(e.getCause)

  def shutdown[F[_] : Concurrent](): F[Unit] = Concurrent[F].delay(client.close())
