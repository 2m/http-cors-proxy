package lt.dvim.httpcorsproxy

import fastparse.core.Parsed.Success
import minitest.SimpleTestSuite

object CookieParserSuite extends SimpleTestSuite {
  import CookieParser._

  test("should parse httponly-av") {
    val Success(_, _) = `httponly-av`.parse("HttpOnly")
  }

  test("should parse path-av") {
    val Success(_, _) = `path-av`.parse("Path=/")
  }

  test("should parse cookie-pair") {
    val Success(parsed, _) = `cookie-pair`.parse("name=value")
    assertEquals(parsed, ("name", "value"))
  }

  test("should parse set-cookie-string") {
    val Success(parsed, _) = `set-cookie-string`.parse("name=value; Path=/; HttpOnly")
    assertEquals(parsed, ("name", "value"))
  }

  test("should parse set-cookie-header") {
    val Success(parsed, _) = `set-cookie-header`.parse("name=value; Path=/; HttpOnly,name2=value2; Path=/test")
    assertEquals(parsed, Seq(("name", "value"), ("name2", "value2")))
  }

  test("should parse set-cookie-header with only path") {
    val Success(parsed, _) = `set-cookie-header`.parse("""name=value; Path=/,name2=value2; Path=/test""")
    assertEquals(parsed, Seq(("name", "value"), ("name2", "value2")))
  }

}
