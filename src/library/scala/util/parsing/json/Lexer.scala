/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2006-2007, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

// $Id$


package scala.util.parsing.json

import scala.util.parsing.combinator._
import scala.util.parsing.combinator.syntactical._
import scala.util.parsing.combinator.lexical._
import scala.util.parsing.input.CharArrayReader.EofCh

/**
 *  @author Derek Chen-Becker <"java"+@+"chen-becker"+"."+"org">
 */
class Lexer extends StdLexical with ImplicitConversions {

  override def token: Parser[Token] =
    ('\"' ~ rep(charSeq | letter) ~ '\"' ^^ lift(StringLit)
    | number ~ letter ^^ { case n ~ l => ErrorToken("Invalid number format : " + n + l) }
    | '-' ~ whitespace ~ number ~ letter ^^ {case ws ~ num ~ l => ErrorToken("Invalid number format : -" + num + l) }
    | '-' ~ whitespace ~ number ^^ {case ws ~ num => NumericLit("-" + num)}
    | number ^^ NumericLit
    | EofCh ^^ EOF
    | delim
    | '\"' ~ failure("Unterminated string")
    | rep(letter) ^^ checkKeyword
    | failure("Illegal character")
    )

  def checkKeyword(xs : List[Any]) = {
    val strRep = xs.mkString("")
    if (reserved.contains(strRep)) Keyword(strRep) else ErrorToken("Not a keyword: " + strRep)
  }

  override def whitespace = rep(whitespaceChar)

  def number  = intPart ~ opt(fracPart) ~ opt(expPart) ^^ { case i ~ f ~ e =>
    i + optString(".",f) + optString("",e)
  }
  def intPart = zero | intList
  def intList = nonzero ~ rep(digit) ^^ {case x ~ y => (x :: y) mkString ""}
  def fracPart = '.' ~ rep(digit) ^^ {x => x.mkString("")}
  def expPart = exponent ~ opt(sign) ~ rep1(digit) ^^ { case e ~ s ~ d =>
    e + optString("",s) + d.mkString("")
  }

  def optString[A](pre: String, a: Option[A]) = a match {
    case Some(x) => pre + x.toString
    case None => ""
  }

  def zero: Parser[String] = '0' ^^ toString
  def nonzero = elem("nonzero digit", d => d.isDigit && d != '0')
  def exponent = elem("exponent character", d => d == 'e' || d == 'E')
  def sign = elem("sign character", d => d == '-' || d == '+')

  def charSeq: Parser[String] =
    ('\\' ~ '\"' ^^ "\""
    |'\\' ~ '\\' ^^ "\\"
    |'\\' ~ '/'  ^^ "/"
    |'\\' ~ 'b'  ^^ "\b"
    |'\\' ~ 'f'  ^^ "\f"
    |'\\' ~ 'n'  ^^ "\n"
    |'\\' ~ 'r'  ^^ "\r"
    |'\\' ~ 't'  ^^ "\t"
    |'\\' ~ 'u' ~ unicodeBlock)

  val hexDigits = Set[Char]() ++ "0123456789abcdefABCDEF".toArray
  def hexDigit = elem("hex digit", hexDigits.contains(_))

 def unicodeBlock = hexDigit ~ hexDigit ~ hexDigit ~ hexDigit ^^ {
   case a ~ b ~ c ~ d =>
     new String(io.UTF8Codec.encode(Integer.parseInt(List(a,b,c,d).mkString(""),16)))
 }

  private def lift[T](f: String => T)(xs: List[Any]): T = f(xs.mkString(""))
}