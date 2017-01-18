package lt.dvim.httpcorsproxy

import minitest.TestSuite

object HttpCorsProxySuite extends TestSuite[NodeJsTestServer] {
  import NodeJsTestServer._

  def setup() = new NodeJsTestServer()
  def tearDown(server: NodeJsTestServer) = server.close()

  test("should proxy a form request") { server =>

    implicit val binding = server.on("GET", "/formRequest") { request =>
      assertEquals(request.body, "username=usr&password=pass")

      val responseHeaders = Map("Set-Cookie" -> Seq("XSRF-TOKEN=123%3D%3D; path=/", "_social_session=987; path=/; HttpOnly"))
      val responseBody = """{ "status": "ok" }"""
      (responseHeaders, responseBody)
    }

    """
    |{
    |  "url": "http://localhost:$port$/formRequest",
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
    |}""".through(HttpCorsProxy.corsProxy) { response =>
      assertEquals(response, """{"cookies":[["XSRF-TOKEN","123%3D%3D"],["_social_session","987"]],"body":{"status":"ok"}}""")
    }

  }

  test("should proxy a request with cookies") { server =>

    implicit val binding = server.on("GET", "/requestWithCookies") { request =>
      assertEquals(request.cookie, Some("cookie1=value"))

      val responseBody = """{ "status": "ok" }"""
      (Map.empty, responseBody)
    }

    """
    |{
    |  "url": "http://localhost:$port$/requestWithCookies",
    |  "cookie": [
    |    "cookie1=value"
    |  ]
    |}
    """.stripMargin.through(HttpCorsProxy.corsProxy) { response =>
      assertEquals(response, """{"cookies":[],"body":{"status":"ok"}}""")
    }

  }

}
