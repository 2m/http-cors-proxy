package lt.dvim.httpcorsproxy

import scala.scalajs.js
import scala.scalajs.js.annotation.ScalaJSDefined

object GoogleCloudFunctions {

  /**
    * Facade of http://expressjs.com/en/api.html#req
    */
  @ScalaJSDefined
  trait Request extends js.Object {

    /**
      * Contains key-value pairs of data submitted in the request body.
      */
    val body: js.Object
  }

  /**
    * Facade of http://expressjs.com/en/api.html#res
    */
  @ScalaJSDefined
  trait Response extends js.Object {

    /**
      * Sets the response’s HTTP header field to value. To set multiple fields at once, pass an object as the parameter.
      */
    def header(field: String, value: String)

    /**
      * Sends the HTTP response.
      */
    def send(body: String): Unit

    /**
      * Ends the response process.
      */
    def end(): Unit

    /**
      * Sets the HTTP status for the response.
      */
    def status(status: Int): Response
  }
}
