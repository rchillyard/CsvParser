/*
 * Copyright (c) 2019. Phasmid Software
 */

package com.phasmidsoftware.render.tag

import scala.language.implicitConversions

case class Attribute(key: String, value: String) {
	override def toString: String = s"""$key="$value""""
}

/**
	* Trait Tag to model an Markup Language-type document.
	*/
trait Tag {

	/**
		* Method to yield the name of this Tag
		*
		* @return the name, that's to say what goes between &lt; and &gt;
		*/
	def name: String

	/**
		* Method to yield the attributes of this Tag.
		*
		* @return a sequence of Attributes
		*/
	def attributes: Seq[Attribute]

	/**
		* Method to yield the content of this Tag.
		*
		* @return the (optional) content as a String.
		*/
	def content: Option[String]

	/**
		* Method to yield the child Tags of this Tag.
		*
		* @return a Seq of Tags.
		*/
	def tags: Seq[Tag]

	/**
		* Method to add a child to this Tag
		*
		* @param tag the tag to be added
		* @return a new version of this Tag with the additional tag added as a child
		*/
	def :+(tag: Tag): Tag

	/**
		* Method to yield the tag names depth-first in a Seq
		*
		* @return a sequence of tag names
		*/
	def \\ : Seq[String] = name +: (for (t <- tags; x <- t.\\) yield x)
}

abstract class BaseTag(name: String, attributes: Seq[Attribute], content: Option[String], tags: Seq[Tag])(implicit rules: TagRules) extends Tag {

	override def toString: String = s"""\n${tagString()}$contentString$tagsString${tagString(true)}"""

	private def attributeString(close: Boolean) = if (close || attributes.isEmpty) "" else " " + attributes.mkString(" ")

	private def tagsString = if (tags.isEmpty) "" else tags mkString ""

	private def nameString(close: Boolean = false) = (if (close) "/" else "") + name

	private def contentString: String = content.getOrElse("")

	private def tagString(close: Boolean = false) = s"<${nameString(close)}${attributeString(close)}>"
}

object Attribute {
	def apply(kv: (String, String)): Attribute = convertFromTuple(kv)

	def mapToAttributes(m: Map[String, String]): Seq[Attribute] = m.toSeq.map(apply)

	implicit def convertFromTuple(kv: (String, String)): Attribute = Attribute(kv._1, kv._2)
}

/**
	* For future expansion.
	* The tag rules will allow us to check the model of a Tag.
	* For example, does it conform to HTML5?
	* Or XML, etc?
	*/
trait TagRules
