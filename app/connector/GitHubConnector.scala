package connector

import cats.data.EitherT
import model.APIError
import play.api.libs.json.Reads
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.Configuration
//dd
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GitHubConnector @Inject()(ws: WSClient, config: Configuration)(implicit ec: ExecutionContext) {

  private val githubToken: Option[String] = config.getOptional[String]("github.token")


  private def getRateLimitHeaders(response: WSResponse): Map[String, String] = {
    Map(
      "X-RateLimit-Limit" -> response.header("X-RateLimit-Limit").getOrElse("Unknown"),
      "X-RateLimit-Remaining" -> response.header("X-RateLimit-Remaining").getOrElse("Unknown"),
      "X-RateLimit-Reset" -> response.header("X-RateLimit-Reset").getOrElse("Unknown")
    )
  }

  // Function to make a GET request to GitHub API
  def get[Response](url: String)(implicit rds: Reads[Response], ec: ExecutionContext): EitherT[Future, APIError, Response] = {
    val request = ws.url(url)

    // Add Authorization header if the GitHub token is available
    val requestWithAuth = githubToken match {
      case Some(token) => request.addHttpHeaders("Authorization" -> s"token $token")
      case None => request
    }

    val response = requestWithAuth.get()

    EitherT {
      response.map { result =>
        // Extract and log rate limit headers
        val rateLimitHeaders = getRateLimitHeaders(result)

        println(s"Rate Limit: ${rateLimitHeaders("X-RateLimit-Limit")}")
        println(s"Remaining Requests: ${rateLimitHeaders("X-RateLimit-Remaining")}")
        println(s"Rate Limit Resets At: ${rateLimitHeaders("X-RateLimit-Reset")}")

        // Handle the response
        result.status match {
          case 200 => Right(result.json.as[Response])
          case _ => Left(APIError.BadAPIResponse(result.status, result.statusText))
        }
      }.recover {
        case _: Throwable => Left(APIError.BadAPIResponse(500, "Could not connect"))
      }
    }
  }
}

