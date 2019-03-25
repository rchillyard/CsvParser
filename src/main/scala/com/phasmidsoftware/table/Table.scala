package com.phasmidsoftware.table

import java.io.{File, InputStream}
import java.net.{URI, URL}

import com.phasmidsoftware.parse.{ParserException, TableParser}

import scala.io.Source
import scala.util.{Failure, Success, Try}

/**
  * A Table of Rows.
  *
  * @tparam Row the type of each row.
  */
trait Table[Row] extends Iterable[Row] {

  def maybeHeader: Option[Seq[String]]

  def map[S](f: Row => S): Table[S] = unit(rows map f, maybeHeader)

  def flatMap[U](f: Row => Table[U]): Table[U] = (rows map f).foldLeft(unit[U](Nil, None))(_ ++ _)

  def unit[S](rows: Seq[S], maybeHeader: Option[Seq[String]]): Table[S]

  def ++[U >: Row](table: Table[U]): Table[U] = unit[U](rows ++ table.rows, for (h1 <- maybeHeader; h2 <- table.maybeHeader) yield h1 ++ h2)

  def rows: Seq[Row]

  def iterator: Iterator[Row] = rows.iterator
}

object Table {

  def parse[T: TableParser](ws: Seq[String]): Try[T] = implicitly[TableParser[T]].parse(ws)

  def parse[T: TableParser](ws: Iterator[String]): Try[T] = parse(ws.toSeq)

  def parse[T: TableParser](x: Source): Try[T] = parse(x.getLines())

  def parse[T: TableParser](u: URI): Try[T] = for (s <- Try(Source.fromURI(u)); t <- parse(s)) yield t

  def parse[T: TableParser](u: URL): Try[T] = parse(u.toURI)

  def parse[T: TableParser](i: InputStream): Try[T] = for (s <- Try(Source.fromInputStream(i)); t <- parse(s)) yield t

  def parse[T: TableParser](f: File): Try[T] = for (s <- Try(Source.fromFile(f)); t <- parse(s)) yield t

  def parseResource[T: TableParser](s: String, clazz: Class[_]): Try[T] =
    clazz.getResource(s) match {
      case null => Failure(ParserException(s"Table.getResource: $s does not exist for $clazz"))
      case u => parse(u)
    }
}

/**
  * CONSIDER eliminating this base class
  *
  * @param rows        the rows of the table
  * @param maybeHeader (optional) header
  * @tparam Row the underlying type of each Row
  */
abstract class BaseTable[Row](rows: Seq[Row], val maybeHeader: Option[Seq[String]]) extends Table[Row] {
  self =>

}

case class TableWithHeader[Row](rows: Seq[Row], header: Seq[String]) extends BaseTable[Row](rows, Some(header)) {
  override def unit[S](rows: Seq[S], maybeHeader: Option[Seq[String]]): Table[S] = maybeHeader match {
    case Some(h) => TableWithHeader(rows, h);
    case _ => throw TableException("header is non-existent")
  }
}

case class TableWithoutHeader[Row](rows: Seq[Row]) extends BaseTable[Row](rows, None) {
  override def unit[S](rows: Seq[S], maybeHeader: Option[Seq[String]]): Table[S] = maybeHeader match {
    case None => TableWithoutHeader(rows);
    case _ => throw TableException("header should be non-existent")
  }
}

case class TableException(w: String) extends Exception(w)