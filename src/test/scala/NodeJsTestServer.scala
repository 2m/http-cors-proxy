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
      if (request.url.takeWhile(_ != '?') == path) {
        var incomingData = Seq.empty[String]
        request
          .onData((data: js.Any) => {
            incomingData :+= data.toString
          })
          .onEnd(() => {
            val requestBody = incomingData.fold("")(_ + _)
            val (responseHeaders, responseBody) =
              r(Request(requestBody, request.headers.get("cookie"), request.url.dropWhile(_ != '?')))

            import js.JSConverters._
            val mapOfJsArray = responseHeaders.mapValues(v => collection.mutable.Seq(v: _*).toJSArray)
            val dict         = collection.mutable.Map(mapOfJsArray.toSeq: _*).toJSDictionary

            response.writeHead(200, dict)
            response.write(responseBody)
            response.end()
          })
      }
    })
    val bindPromise = Promise[Binding]
    server.listen(0, "localhost", 511,
      (args: js.Any) => {
      bindPromise.success(Binding(server.address.port.get))
    })
    bindPromise.future
  }

  def close() = server.close()
}

object NodeJsTestServer {
  case class Binding(port: Int)
  case class Response(status: Int, body: Option[String], headers: Map[String, String])
  case class Request(body: String, cookie: Option[String], query: String)

  @ScalaJSDefined
  class PreparedRequest(request: String) extends GoogleCloudFunctions.Request {
    override val body = js.JSON.parse(request).asInstanceOf[js.Object]
  }

  @ScalaJSDefined
  class ResponseListener() extends GoogleCloudFunctions.Response {
    private var statusCode = Option.empty[Int]
    private var headers    = Map.empty[String, String]

    val response = Promise[Response]

    override def header(field: String, value: String): Unit =
      headers += field -> value

    override def status(status: Int): GoogleCloudFunctions.Response = {
      statusCode = Some(status)
      this
    }

    override def send(resp: String): Unit =
      response.success(Response(statusCode.getOrElse(200), Some(resp), headers))

    override def end(): Unit =
      response.success(Response(statusCode.get, None, headers))
  }

  implicit class RequestStringOps(s: String) {
    def through(f: (GoogleCloudFunctions.Request, GoogleCloudFunctions.Response) => Unit)(
        responseAssertion: Response => Unit)(implicit b: Future[Binding]): Future[Unit] = {
      import scala.concurrent.ExecutionContext.Implicits.global

      b.flatMap { binding =>
        handleRequest(f, s.replace("$port$", binding.port.toString).stripMargin, responseAssertion)
      }
    }

    def throughNoServer(f: (GoogleCloudFunctions.Request, GoogleCloudFunctions.Response) => Unit)(
        responseAssertion: Response => Unit): Future[Unit] =
      handleRequest(f, s, responseAssertion)

    private def handleRequest(f: (GoogleCloudFunctions.Request, GoogleCloudFunctions.Response) => Unit,
                              request: String,
                              responseAssertion: Response => Unit) = {
      import scala.concurrent.ExecutionContext.Implicits.global

      val gcfRequest  = new PreparedRequest(request)
      val gcfResponse = new ResponseListener()
      f(gcfRequest, gcfResponse)
      gcfResponse.response.future.map(responseAssertion)
    }
  }
}
