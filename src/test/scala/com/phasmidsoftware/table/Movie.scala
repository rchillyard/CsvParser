package com.phasmidsoftware.table

import com.phasmidsoftware.parse._

import scala.util.Try
import scala.util.matching.Regex

/**
  * This class represents a Movie from the IMDB data file on Kaggle.
  * Although the limitation on 22 fields in a case class has partially gone away, it's still convenient to group the different attributes together into logical classes.
  *
  * Created by scalaprof on 9/12/16.
  *
  * Common questions in this assignment:
  * 1. Where is main method?
  * In most case, you don't need to run main method for assignments.
  * Unit tests are provided to test your implementation.
  * In this assignment, you will find the `object Movie extends App`,
  * the `App` trait can be used to quickly turn objects into executable programs.
  * You can read the official doc of Scala for more details.
  *
  * 2. How to understand the whole program in this assignment?
  * I won't suggest you to understand the whole program in this assignment,
  * there are some advanced features like `implicit` which hasn't been covered in class.
  * You should be able to understand it before midterm.
  * I will suggest you only focus on each TO BE IMPLEMENTED in the assignments.
  *
  */
case class Movie(title: String, format: Format, production: Production, reviews: Reviews, director: Principal, actor1: Principal, actor2: Principal, actor3: Principal, genres: AttributeSet, plotKeywords: AttributeSet, imdb: String)

/**
  * The movie format (including language and duration).
  *
  * @param color        whether filmed in color
  * @param language     the native language of the characters
  * @param aspect_ratio the aspect ratio of the film
  * @param duration     its length in minutes
  */
case class Format(color: String, language: String, aspect_ratio: Double, duration: Int) {
  override def toString: String = {
    s"$color,$language,$aspect_ratio,$duration"
  }
}

/**
  * The production: its country, year, and financials
  *
  * @param country    country of origin
  * @param budget     production budget in US dollars
  * @param gross      gross earnings (?)
  * @param title_year the year the title was registered (?)
  */
case class Production(country: String, budget: Option[Int], gross: Int, title_year: Int) {
  def isKiwi: Boolean = this match {
    case Production("New Zealand", _, _, _) => true
    case _ => false
  }
}

/**
  * Information about various forms of review, including the content rating.
  */
case class Reviews(imdbScore: Double, facebookLikes: Int, contentRating: Rating, numUsersReview: Int, numUsersVoted: Int, numCriticReviews: Int, totalFacebookLikes: Int)

/**
  * A cast or crew principal
  *
  * @param name          name
  * @param facebookLikes number of FaceBook likes
  */
case class Principal(name: Name, facebookLikes: Int) {
  override def toString = s"$name ($facebookLikes likes)"
}

/**
  * A name of a contributor to the production
  *
  * @param first  first name
  * @param middle middle name or initial
  * @param last   last name
  * @param suffix suffix
  */
case class Name(first: String, middle: Option[String], last: String, suffix: Option[String]) {
  override def toString: String = {
    case class Result(r: StringBuffer) {
      def append(s: String): Unit = r.append(" " + s)

      override def toString: String = r.toString
    }
    val r: Result = Result(new StringBuffer(first))
    middle foreach {
      r.append
    }
    r.append(last)
    suffix foreach {
      r.append
    }
    r.toString
  }
}

/**
  * The US rating
  */
case class Rating(code: String, age: Option[Int]) {
  override def toString: String = code + (age match {
    case Some(x) => "-" + x
    case _ => ""
  })
}

object MovieFormat extends Formats {

  implicit val movieColumnHelper: ColumnHelper[Movie] = columnHelper(
    "title" -> "movie_title",
    "actor1" -> "actor_1",
    "actor2" -> "actor_2",
    "actor3" -> "actor_3",
    "plotKeywords" -> "plot_keywords",
    "imdb" -> "movie_imdb_link")
  implicit val reviewsColumnHelper: ColumnHelper[Reviews] = columnHelper(
    "imdbScore" -> "imdb_score",
    "facebookLikes" -> "movie_facebook_likes",
    "contentRating" -> "content_rating",
    "numUsersReview" -> "num_user_for_reviews",
    "numUsersVoted" -> "num_voted_users",
    "numCriticReviews" -> "num_critic_for_reviews",
    "totalFacebookLikes" -> "cast_total_facebook_likes")
  implicit val ratingColumnHelper: ColumnHelper[Rating] = columnHelper()
  implicit val formatColumnHelper: ColumnHelper[Format] = columnHelper()
  implicit val productionColumnHelper: ColumnHelper[Production] = columnHelper()
  implicit val principalColumnHelper: ColumnHelper[Principal] = columnHelper(Some("$x_$c"), "facebookLikes" -> "facebook_likes")
  implicit val multiColumnHelper: ColumnHelper[AttributeSet] = columnHelper()
  // CONSIDER can we use ParseableStringList here?
  implicit val listFormat: CellParser[StringList] = cellReader(Parseable.split)
  val fRating: String => Rating = Rating.apply
  implicit val ratingFormat: CellParser[Rating] = cellReader(fRating)
  implicit val formatFormat: CellParser[Format] = cellReader4(Format.apply)
  implicit val productionFormat: CellParser[Production] = cellReader4(Production.apply)
  val fPrincipal: (String, Int) => Principal = Principal.apply
  implicit val principalFormat: CellParser[Principal] = cellReader2(fPrincipal)
  implicit val reviewsFormat: CellParser[Reviews] = cellReader7(Reviews.apply)
  val fAttributes: String => AttributeSet = AttributeSet.apply
  implicit val attributesFormat: CellParser[AttributeSet] = cellReader(fAttributes)
  implicit val movieFormat: CellParser[Movie] = cellReader11(Movie.apply)

  implicit object MovieConfig extends DefaultRowConfig {
    override val string: Regex = """[^\,]*""".r
    override val delimiter: Regex = """,""".r
    override val listEnclosure: String = ""
  }

  implicit val parser: StandardRowParser[Movie] = StandardRowParser[Movie](LineParser.apply)

  implicit object MovieTableParser extends TableParser[Table[Movie]] {
    type Row = Movie

    def hasHeader: Boolean = true

    override def forgiving: Boolean = true

    def rowParser: RowParser[Row] = implicitly[RowParser[Row]]

    def builder(rows: Seq[Row]): Table[Movie] = TableWithoutHeader(rows)
  }
}

object Format {
  def apply(params: List[String]): Format = params match {
    case color :: language :: aspectRatio :: duration :: Nil => apply(color, language, aspectRatio.toDouble, duration.toInt)
    case _ => throw new Exception(s"logic error in Format: $params")
  }
}

object Production {
  def apply(params: List[String]): Production = params match {
    case country :: budget :: gross :: titleYear :: Nil => apply(country, Try(budget.toInt).toOption, gross.toInt, titleYear.toInt)
    case _ => throw new Exception(s"logic error in Production: $params")
  }
}

object Reviews {
  def apply(params: List[String]): Reviews = params match {
    case imdbScore :: facebookLikes :: contentRating :: numUsersReview :: numUsersVoted :: numCriticReviews :: totalFacebookLikes :: Nil => apply(imdbScore.toDouble, facebookLikes.toInt, Rating(contentRating), numUsersReview.toInt, numUsersVoted.toInt, numCriticReviews.toInt, totalFacebookLikes.toInt)
    case _ => throw new Exception(s"logic error in Reviews: $params")
  }
}

object Name {
  // this regex will not parse all names in the Movie database correctly. Still, it gets most of them.
  private val rName =
    """^([\p{L}\-\']+\.?)\s*(([\p{L}\-]+\.)\s)?([\p{L}\-\']+\.?)(\s([\p{L}\-]+\.?))?$""".r

  def apply(name: String): Name = name match {
    case rName(first, _, null, last, _, null) => apply(first, None, last, None)
    case rName(first, _, middle, last, _, null) => apply(first, Some(middle), last, None)
    case rName(first, _, null, last, _, suffix) => apply(first, None, last, Some(suffix))
    case rName(first, _, middle, last, _, suffix) => apply(first, Some(middle), last, Some(suffix))
    case _ => throw new Exception(s"parse error in Name: $name")
  }
}

object Principal {
  def apply(params: List[String]): Principal = params match {
    case name :: facebookLikes :: Nil => apply(name, facebookLikes.toInt)
    case _ => throw new Exception(s"logic error in Principal: $params")
  }

  def apply(name: String, facebookLikes: Int): Principal = apply(Name(name), facebookLikes)
}

object Rating {
  /**
    * Alternative apply method for the Rating class such that a single String is decoded
    *
    * @param s a String made up of a code, optionally followed by a dash and a number, e.g. "R" or "PG-13"
    * @return a Rating
    */
  def apply(s: String): Rating =
    s match {
      case rRating(code, _, null) => apply(code, None)
      case rRating(code, _, age) => apply(code, Try(age.toInt).toOption)
      case _ => throw new Exception(s"parse error in Rating: $s")
    }

  private val rRating = """^(\w*)(-(\d\d))?$""".r

}
