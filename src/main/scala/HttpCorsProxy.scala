package lt.dvim.httpcorsproxy

import fastparse.core.Parsed
import fr.hmil.roshttp.body.URLEncodedBody
import fr.hmil.roshttp.{HttpRequest, Method}
import upickle.Js
import upickle.default._

import scala.scalajs.js
import scala.scalajs.js.annotation._
import scala.util.{Failure, Success}

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
  case class Request(url: String, cookie: Option[String] = Option.empty, form: Seq[FormData] = Seq.empty)
  case class Response(cookies: Seq[(String, String)], body: Js.Obj)

  @JSExportTopLevel("corsProxy")
  def corsProxy(req: GoogleCloudFunctions.Request, res: GoogleCloudFunctions.Response) = {
    println("inside of a function")
    import monix.execution.Scheduler.Implicits.global

    val request = read[Request](req.body)

    val outBoundRequest = HttpRequest(request.url)
      .withMethod(Method.POST)
      .withBody(URLEncodedBody(request.form.map(f => (f.name, f.value)): _*))

    request.cookie
      .fold(outBoundRequest)(outBoundRequest.withHeader("Cookie", _))
      .send().onComplete {e
        case Success(response) =>
          // without the back and forth conversion we get the following error:
          // An undefined behavior was detected: "..." is not an instance of java.lang.String
          val cookieHeaderValue = response.headers.get("Set-Cookie").getOrElse("").asInstanceOf[js.Any].toString
          val cookies = CookieParser.`set-cookie-header`.parse(cookieHeaderValue) match {
            case Parsed.Success(parsed, _) => parsed
            case Parsed.Failure(_, _, _) => Seq.empty
          }
          res.send(write(Response(cookies, read[Js.Obj](response.body))))
        case Failure(ex) =>
          ex.printStackTrace()
          res.status(400)
      }
  }

}
