package lt.dvim.httpcorsproxy

import org.scalajs.nodejs.http.{Http, IncomingMessage, ServerResponse}

import scala.concurrent.{Future, Promise}
import scala.scalajs.js
import scala.scalajs.js.annotation.ScalaJSDefined

class NodeJsTestServer {

  import NodeJsTestServer._

  val server = Http().createServer()

  def on(method: String, path: String)(r: Request => (Map[String, Seq[String]], String)) = {
    server.onRequest((request: IncomingMessage, response: ServerResponse) => {
      println("inside request handler")
      if (request.url == path) {
        var incomingData = Seq.empty[String]
        request.onData((data: js.Any) => {
          incomingData :+= data.toString
        }).onEnd(() => {
          val requestBody = incomingData.fold("")(_ + _)
          val (responseHeaders, responseBody) = r(Request(requestBody, request.headers.get("cookie")))

          import js.JSConverters._
          val mapOfJsArray = responseHeaders.mapValues(v => collection.mutable.Seq(v:_*).toJSArray)
          val dict = collection.mutable.Map(mapOfJsArray.toSeq: _*).toJSDictionary

          response.writeHead(200, dict)
          response.write(responseBody)
          response.end()
        })
      }
    })
    val bindPromise = Promise[Binding]
    server.listen(0, "localhost", 511, (args: js.Any) => {
      println(s"listening to ${server.address.port.get}")
      bindPromise.success(Binding(server.address.port.get))
    })
    bindPromise.future
  }

  def close() = server.close()
}

object NodeJsTestServer {
  case class Binding(port: Int)
  case class Request(body: String, cookie: Option[String])

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

  implicit class RequestStringOps(s: String) {
    def through(f: (GoogleCloudFunctions.Request, GoogleCloudFunctions.Response) => Unit)(responseAssertion: String => Unit)(implicit b: Future[Binding]): Future[Unit] = {
      import scala.concurrent.ExecutionContext.Implicits.global

      b.flatMap { binding =>
        val gcfRequest = new PreparedRequest(s.replace("$port$", binding.port.toString).stripMargin)
        val gcfResponse = new ResponseListener()
        println("calling gcf handler")
        f(gcfRequest, gcfResponse)
        gcfResponse.response.future.map(responseAssertion)
      }

    }
  }
}
