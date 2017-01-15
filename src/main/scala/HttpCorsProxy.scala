package lt.dvim.httpcorsproxy

import fr.hmil.roshttp.{HttpRequest, Method}
import fr.hmil.roshttp.body.URLEncodedBody

import scala.scalajs.js
import scala.scalajs.js.annotation._
import scala.util.{Failure, Success}
import upickle.default._

import scala.concurrent.ExecutionContext

object GoogleCloudFunctions {

  @ScalaJSDefined
  trait Request extends js.Object {
    val body: String
  }

  @ScalaJSDefined
  trait Response extends js.Object {
    def send(resp: String): Unit
    def end(): Unit
    def status(status: Int): Response
  }
}

object HttpCorsProxy {

  case class FormData(name: String, value: String)
  case class Request(url: String, form: Seq[FormData])
  case class Response(cookies: Seq[(String, String)])

  @JSExportTopLevel("corsProxy")
  def corsProxy(req: GoogleCloudFunctions.Request, res: GoogleCloudFunctions.Response)(implicit ec: ExecutionContext) = {
    import monix.execution.Scheduler.Implicits.global

    val request = read[Request](req.body)

    HttpRequest(request.url)
      .withMethod(Method.POST)
      .withBody(URLEncodedBody(request.form.map(f => (f.name, f.value)): _*))
      .send().onComplete {
        case Success(response) =>
          println("processing success")
          val cookies = response.headers.iterator.collect {
            case ("Set-Cookie", value) =>
              val separatorIdx = value.indexOf("=")
              value.take(separatorIdx) -> value.drop(separatorIdx + 1)
          }
          res.send(write(Response(cookies.toSeq)))
        case Failure(ex) =>
          ex.printStackTrace()
          res.status(400)
      }
  }

}
