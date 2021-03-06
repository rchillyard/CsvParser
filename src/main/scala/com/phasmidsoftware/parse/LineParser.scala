/*
 * Copyright (c) 2019. Phasmid Software
 */

package com.phasmidsoftware.parse

import org.slf4j.{Logger, LoggerFactory}

import scala.annotation.tailrec
import scala.util.Try
import scala.util.matching.Regex
import scala.util.parsing.combinator.JavaTokenParsers

/**
  * LineParser: class to parse lines of a CSV file.
  * NOTE: list elements always appear as a string in the form { element0 , element1 , ... }
  *
  * @param delimiter     a Regex used to match a delimiter between cells in a row.
  * @param string        a Regex used to match the content of a cell.
  * @param enclosures    the enclosure characters around a list (if any).
  * @param listSeparator the list separator character.
  * @param quote         the quote character which is able to preempt the string regex:
  *                      between two quote characters,
  *                      there can be any number of any character (other than quote).
  * @param verbose       will print the various parameters.
  */
class LineParser(delimiter: Regex, string: Regex, enclosures: String, listSeparator: Char, quote: Char, verbose: Boolean = false) extends JavaTokenParsers {

  if (verbose) LineParser.logger.info(s"delimiter: '${delimiter.regex}', string: '${string.regex}', enclosures: '$enclosures', quote: '$quote', listSeparator: '$listSeparator', ")
  runChecks()

  override def skipWhitespace: Boolean = false

  def parseRow(w: String): Try[Strings] = parseAll(row, w) match {
    case Success(s, _) => scala.util.Success(s)
    case Failure(x, _) => scala.util.Failure(formException(w, x))
    case Error(x, _) => scala.util.Failure(formException(w, x))
  }

  lazy val row: Parser[Strings] = rep1sep(cell, delimiter)

  lazy val cell: Parser[String] = quotedString | list | string | failure("invalid string")

  lazy val quotedString: Parser[String] = quotedStringWithQuotes | pureQuotedString

  lazy val pureQuotedString: Parser[String] = quote ~> stringInQuotes <~ quote

  lazy val stringInQuotes: Parser[String] = s"""[^$quote]*""".r

  lazy val quotedStringWithQuotes: Parser[String] = quotedStringWithQuotesAsList ^^ (ws => ws.mkString(s"$quote"))

  lazy val quotedStringWithQuotesAsList: Parser[Seq[String]] = quote ~> repsep(stringInQuotes, s"$quote$quote") <~ quote

  lazy val list: Parser[String] = getOpenChar ~> (component ~ listSeparator ~ rep1sep(component, listSeparator)) <~ getCloseChar ^^ { case x ~ _ ~ xs => (x +: xs).mkString("{", ",", "}") }

  private lazy val component: Parser[String] = s"""[^,$listSeparator}]+""".r

  private lazy val getOpenChar: Parser[String] = s"${enclosures.headOption.getOrElse("")}"

  private lazy val getCloseChar: Parser[String] = s"${enclosures.lastOption.getOrElse("")}"

  private def formException(row: String, x: String) = ParserException(s"Cannot parse row '$row' due to: $x")

  override def toString: String = s"""LineParser: delimiter=$delimiter, string=$string, listSeparator='$listSeparator', enclosures='$enclosures', quote="$quote""""

  private lazy val getDelimiterChar: Char = {
    @tailrec
    def inner(w: Seq[Char], escaped: Boolean): Char =
      w match {
        case h :: t =>
          if (escaped) h match {
            case 't' => '\t'
            case '\\' => '\\'
            case 'n' => '\n'
            case 'r' => '\r'
            case 'd' => '0'
            case 'f' => '\f'
            case 'b' => '\b'
            case _ => h
          }
          else h match {
            case '[' | '{' => inner(t, escaped = false)
            case '\\' => inner(t, escaped = true)
            case '^' => throw ParserException(s"Cannot get a delimiter from ${delimiter.regex} (unsupported)")
            case _ => h
          }
        case Nil => throw ParserException(s"Cannot get a delimiter from ${delimiter.regex}")
      }

    inner(delimiter.regex.toList, escaped = false)
  }

  private def runChecks(): Unit = {
    def check[X](parser: Parser[X], input: String, matchedValue: X) = Try(parseAll(parser, input) match {
      case Success(`matchedValue`, _) =>
      case Failure(z, _) => throw ParserException(s"Warning: (LineParser constructor validity check): '$input' did not result in $matchedValue because: $z")
      case Error(z, _) => throw ParserException(s"Warning: (LineParser constructor validity check): '$input' did not result in $matchedValue because: $z")
    })

    implicit class Trial(x: Try[Unit]) {
      def squawk(): Unit = x match {
        case scala.util.Failure(z) => LineParser.logger.warn(s"squawk: $z")
        case _ =>
      }

      def &&(y: Trial): Trial =
        if (x.isSuccess) y else x
    }

    (
      check(cell, "Hello", "Hello") &&
        //        check(cell, "http://www.imdb.com/title/tt0499549/?ref_=fn_tt_tt_1", "http://www.imdb.com/title/tt0499549/?ref_=fn_tt_tt_1") &&
        check(quotedString, s"""${quote}Hello${getDelimiterChar}Goodbye$quote""", s"""Hello${getDelimiterChar}Goodbye""")
      ).squawk()
  }

}

object LineParser {
  def apply(implicit c: RowConfig): LineParser = {
    LineParser.logger.info(s"Constructing LineParser with an implicitly defined instance of RowConfig: $c")
    new LineParser(c.delimiter, c.string, c.listEnclosure, c.listSep, c.quote)
  }

  val logger: Logger = LoggerFactory.getLogger(LineParser.getClass)
}

case class ParserException(msg: String, e: Throwable = null) extends Exception(msg, e)