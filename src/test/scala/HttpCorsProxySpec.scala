package lt.dvim.httpcorsproxy

import lt.dvim.httpcorsproxy.HttpCorsProxy.Request
import org.scalajs.nodejs.http.{ClientRequest, Http, ServerResponse}
import org.scalatest.{AsyncWordSpec, Matchers, WordSpec}
import org.scalatest.concurrent.ScalaFutures
import upickle.default._

import scala.concurrent.Promise
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, ScalaJSDefined}
import scala.concurrent.duration._

class HttpCorsProxySpec extends AsyncWordSpec with Matchers {
  import HttpCorsProxySpec._

//  implicit val defaultPatience =
//    PatienceConfig(timeout = 5.seconds, interval = 100.millis)

  "cors proxy" should {
    "proxy a request" in {

      val request = """
        |{
        |  "url": "http://localhost:8888",
        |  "form": [
        |    {
        |      "name": "username",
        |      "value": "usr"
        |    },
        |    {
        |      "name": "password",
        |      "value": "pass"
        |    }
        |  ]
        |}
      """.stripMargin

      val http = Http()
      val server = http.createServer((request: ClientRequest, response: ServerResponse) => {
        response.writeHead(200, js.Dictionary("Set-Cookie" -> "value=123"))
        println("in the server")
        response.write("Hello World")
        response.end()
      })

      server.listen(8888)

      val gcfResponse = new ResponseListener()
      HttpCorsProxy.corsProxy(new PreparedRequest(request), gcfResponse)

      gcfResponse.response.future.map(_ shouldBe "opapa")(monix.execution.Scheduler.Implicits.global)
    }
  }

}

object HttpCorsProxySpec {

  @ScalaJSDefined
  class PreparedRequest(request: String) extends GoogleCloudFunctions.Request {
    override val body = request
  }

  @ScalaJSDefined
  class ResponseListener() extends GoogleCloudFunctions.Response {
    val response = Promise[String]

    override def send(resp: String): Unit =
      response.success(resp)

    override def end(): Unit =
      ???

    override def status(status: Int): GoogleCloudFunctions.Response =
      ???
  }

}
