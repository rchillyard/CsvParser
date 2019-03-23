package com.phasmidsoftware.tableparser

import com.phasmidsoftware.format.FP

import scala.util.{Failure, Try}

trait TableParser[Table] {

  type Row

  def hasHeader: Boolean

  def rowParser: RowParser[Row]

  def builder(rows: Seq[Row]): Table

  // CONSIDER returning Table unwrapped and using lifted conversion functions
  def parse(ws: Seq[String]): Try[Table] = {
    def parseRows(header: Seq[String], ws1: Seq[String]): Try[Table] =
      for (rs <- FP.sequence(for (w <- ws1) yield rowParser.parse(w)(header))) yield builder(rs)

    def separateHeaderAndRows(h: String, t: Seq[String]) = for (ws <- rowParser.parseHeader(h); rs <- parseRows(ws, t)) yield rs

    if (hasHeader) ws match {
      case h #:: t => separateHeaderAndRows(h, t)
      case h :: t => separateHeaderAndRows(h, t)
      case _ => Failure(ParserException("no rows to parse"))
    }
    else parseRows(Nil, ws)
  }
}
