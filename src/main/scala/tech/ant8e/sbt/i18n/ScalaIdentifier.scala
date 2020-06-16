package tech.ant8e.sbt.i18n

import scala.language.experimental.macros

object ScalaIdentifier {

  val plainIdTest = "\\p{Alpha}(\\p{Alnum}|_)*".r

  def asIdentifier(s: String): String =
    s match {
      case plainIdTest(_*) if !scalaKeywords.contains(s) => s
      case ""                                            => ""
      case _                                             => s"`$s`"
    }

  val scalaKeywords = Seq(
    "abstract",
    "case",
    "catch",
    "class",
    "def",
    "do",
    "else",
    "extends",
    "false",
    "final",
    "finally",
    "for",
    "forSome",
    "if",
    "implicit",
    "import",
    "lazy",
    "match",
    "new",
    "null",
    "object",
    "override",
    "package",
    "private",
    "protected",
    "return",
    "sealed",
    "super",
    "this",
    "throw",
    "trait",
    "try",
    "true",
    "type",
    "val",
    "var",
    "while",
    "with",
    "yield",
    ":",
    "=",
    "=>",
    "<-",
    "<:",
    "<%",
    ">:",
    "#",
    "@"
  )
}
