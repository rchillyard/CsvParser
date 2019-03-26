package com.phasmidsoftware.table

import org.scalatest.{FlatSpec, Matchers}

import scala.util._

//noinspection SpellCheckingInspection
class MovieSpec extends FlatSpec with Matchers {

  behavior of "Movie table"

  it should "read the first movie from the IMDB dataset" in {
    import MovieFormat._

    val movies = Seq(
      "color,director_name,num_critic_for_reviews,duration,director_facebook_likes,actor_3_facebook_likes,actor_2_name,actor_1_facebook_likes,gross,genres,actor_1_name,movie_title,num_voted_users,cast_total_facebook_likes,actor_3_name,facenumber_in_poster,plot_keywords,movie_imdb_link,num_user_for_reviews,language,country,content_rating,budget,title_year,actor_2_facebook_likes,imdb_score,aspect_ratio,movie_facebook_likes",
      "Color,James Cameron,723,178,0,855,Joel David Moore,1000,760505847,Action|Adventure|Fantasy|Sci-Fi,CCH Pounder,Avatar,886204,4834,Wes Studi,0,avatar|future|marine|native|paraplegic,http://www.imdb.com/title/tt0499549/?ref_=fn_tt_tt_1,3054,English,USA,PG-13,237000000,2009,936,7.9,1.78,33000"
    )

    val x: Try[Table[Movie]] = for (r <- Table.parse(movies)) yield r
    x should matchPattern { case Success(TableWithoutHeader(_)) => }
    val mt = x.get
    println(s"Movie: successfully read ${mt.size} rows")
    println(mt)
    mt.size shouldBe 1
  }

  it should "read the first (edited) movie from the IMDB dataset" in {
    import MovieFormat._

    val movies = Seq(
      "color,director_name,num_critic_for_reviews,duration,director_facebook_likes,actor_3_facebook_likes,actor_2_name,actor_1_facebook_likes,gross,genres,actor_1_name,movie_title,num_voted_users,cast_total_facebook_likes,actor_3_name,facenumber_in_poster,plot_keywords,movie_imdb_link,num_user_for_reviews,language,country,content_rating,budget,title_year,actor_2_facebook_likes,imdb_score,aspect_ratio,movie_facebook_likes",
      "Color,James Cameron,723,178,0,855,Joel David Moore,1000,760505847,Action|Adventure|Fantasy|Sci-Fi,CCH Pounder,Avatar,886204,4834,Wes Studi,0,avatar|future|marine|native|paraplegic,http://www.imdb.com/title/tt0499549/?ref_=fn_tt_tt_1,3054,English,USA,PG-13,,2009,936,7.9,1.78,33000"
    )

    val x: Try[Table[Movie]] = for (r <- Table.parse(movies)) yield r
    x should matchPattern { case Success(TableWithoutHeader(_)) => }
    x.get.size shouldBe 0
  }

  it should "fail to read the first (edited) movie from the IMDB dataset" in {
    import MovieFormat._

    implicit object MovieTableParser extends MovieTableParser {
      override def forgiving: Boolean = false
    }

    val movies = Seq(
      "color,director_name,num_critic_for_reviews,duration,director_facebook_likes,actor_3_facebook_likes,actor_2_name,actor_1_facebook_likes,gross,genres,actor_1_name,movie_title,num_voted_users,cast_total_facebook_likes,actor_3_name,facenumber_in_poster,plot_keywords,movie_imdb_link,num_user_for_reviews,language,country,content_rating,budget,title_year,actor_2_facebook_likes,imdb_score,aspect_ratio,movie_facebook_likes",
      "Color,James Cameron,723,178,0,855,Joel David Moore,1000,760505847,Action|Adventure|Fantasy|Sci-Fi,CCH Pounder,Avatar,886204,4834,Wes Studi,0,avatar|future|marine|native|paraplegic,http://www.imdb.com/title/tt0499549/?ref_=fn_tt_tt_1,3054,English,USA,PG-13,,2009,936,7.9,1.78,33000"
    )

    val x: Try[Table[Movie]] = for (r <- Table.parse(movies)) yield r
    x should matchPattern { case Failure(_) => }
  }

}