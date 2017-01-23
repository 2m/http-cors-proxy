package lt.dvim.httpcorsproxy

import fastparse.all._

object CookieParser {

  // simplified https://tools.ietf.org/html/rfc6265#section-4.1.1
  val `httponly-av`    = P(IgnoreCase("HttpOnly"))
  val `path-value`     = P(CharPred(!";,".contains(_)).rep)
  val `path-av`        = P(IgnoreCase("Path=") ~ `path-value`)
  val `cookie-av`      = P(`path-av` | `httponly-av`)
  val `non-separators` = P(CharPred(!("""()<>@,;:\"/[]?={}""" + '\t').contains(_)))
  val token            = P(`non-separators`.rep(1))
  val `cookie-octet` =
    P(CharPred(c => !Character.isISOControl(c) && !Character.isWhitespace(c) && !""",";\""".contains(c)))
  val `cookie-value`      = P(`cookie-octet`.rep)
  val `cookie-name`       = token
  val `cookie-pair`       = P(`cookie-name`.! ~ "=" ~ `cookie-value`.!)
  val `set-cookie-string` = P(`cookie-pair` ~ ("; " ~ `cookie-av`).rep)
  val `set-cookie-header` = P(`set-cookie-string`.rep(1, sep = ",") ~ End)

}
