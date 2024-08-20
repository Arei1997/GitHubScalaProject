package service

import com.google.inject.Inject
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient
import scala.concurrent.{ExecutionContext, Future}

class NewsService @Inject()(ws: WSClient, config: Configuration)(implicit ec: ExecutionContext) {

  // Retrieve the API key from the environment variable
  private val apiKey: String = config.get[String]("newsapi.key")

  // Fetch news based on a specific category
  def fetchNewsByCategory(category: String): Future[Seq[NewsArticle]] = {
    val url = s"https://newsapi.org/v2/everything?q=$category&apiKey=$apiKey"
    ws.url(url).get().map { response =>
      val json = response.json
      (json \ "articles").as[Seq[JsValue]].map { articleJson =>
        NewsArticle(
          title = (articleJson \ "title").as[String],
          description = (articleJson \ "description").as[String],
          url = (articleJson \ "url").as[String],
          sourceName = (articleJson \ "source" \ "name").as[String]
        )
      }
    }
  }

  // Default method to fetch tech-related news
  def fetchTechNews(): Future[Seq[NewsArticle]] = fetchNewsByCategory("technology")
}

case class NewsArticle(title: String, description: String, url: String, sourceName: String)

object NewsArticle {
  implicit val format = Json.format[NewsArticle]
}
