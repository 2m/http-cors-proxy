package lt.dvim.httpcorsproxy

import minitest.TestSuite

object HttpCorsProxySuite extends TestSuite[NodeJsTestServer] {
  import NodeJsTestServer._

  def setup()                            = new NodeJsTestServer()
  def tearDown(server: NodeJsTestServer) = server.close()

  test("should proxy a form request") { server =>
    implicit val binding = server.on("POST", "/formRequest") { request =>
      assertEquals(request.body, "username=usr&password=pass")

      val responseHeaders =
        Map("Set-Cookie" -> Seq("XSRF-TOKEN=123%3D%3D; path=/", "_social_session=987; path=/; HttpOnly"))
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
      assertEquals(response.headers("Access-Control-Allow-Origin"), "*")
      assertEquals(response.body.get,
        """{"cookies":[["XSRF-TOKEN","123%3D%3D"],["_social_session","987"]],"body":{"status":"ok"}}""")
    }
  }

  test("should proxy a request with cookies") { server =>
    implicit val binding = server.on("POST", "/requestWithCookies") { request =>
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
    """.through(HttpCorsProxy.corsProxy) { response =>
      assertEquals(response.headers("Access-Control-Allow-Origin"), "*")
      assertEquals(response.body.get, """{"cookies":[],"body":{"status":"ok"}}""")
    }
  }

  test("should proxy a request with query parameters") { server =>
    implicit val binding = server.on("POST", "/requestWithQueryParams") { request =>
      assertEquals(request.query, "?param1=value1&param1=value12&param2=value2")

      val responseBody = """{ "status": "ok" }"""
      (Map.empty, responseBody)
    }

    """
      |{
      |  "url": "http://localhost:$port$/requestWithQueryParams",
      |  "query": [
      |    {
      |      "name": "param1",
      |      "value": "value1"
      |    },
      |    {
      |      "name": "param1",
      |      "value": "value12"
      |    },
      |    {
      |      "name": "param2",
      |      "value": "value2"
      |    }
      |  ]
      |}
    """.through(HttpCorsProxy.corsProxy) { response =>
      assertEquals(response.headers("Access-Control-Allow-Origin"), "*")
      assertEquals(response.body.get, """{"cookies":[],"body":{"status":"ok"}}""")
    }
  }

  test("should proxy a GET request") { server =>
    implicit val binding = server.on("GET", "/getRequest") { request =>
      val responseBody = """{ "status": "ok" }"""
      (Map.empty, responseBody)
    }

    """
      |{
      |  "method": "GET",
      |  "url": "http://localhost:$port$/getRequest"
      |}
    """.through(HttpCorsProxy.corsProxy) { response =>
      assertEquals(response.headers("Access-Control-Allow-Origin"), "*")
      assertEquals(response.body.get, """{"cookies":[],"body":{"status":"ok"}}""")
    }
  }

  test("should return 400 on badly formed response from server") { server =>
    implicit val binding = server.on("POST", "/requestWithBadResponse") { request =>
      val responseBody = """{ "status": "ok with missing double quote }"""
      (Map.empty, responseBody)
    }

    """
      |{
      |  "url": "http://localhost:$port$/requestWithBadResponse"
      |}
    """.through(HttpCorsProxy.corsProxy) { response =>
      assertEquals(response.status, 400)
      assert(response.body.get.startsWith("ERROR: "))
    }
  }

  test("should return 400 on badly formed request") { server =>
    "{}".throughNoServer(HttpCorsProxy.corsProxy) { response =>
      assertEquals(response.status, 400)
      assert(response.body.get.startsWith("ERROR: "))
    }
  }

}
