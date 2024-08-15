package connector

import cats.data.EitherT
import model.APIError
import play.api.libs.json.Reads
import play.api.libs.ws.WSClient
import play.api.Configuration

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GitHubConnector @Inject()(ws: WSClient, config: Configuration)(implicit ec: ExecutionContext) {

  private val githubToken: Option[String] = config.getOptional[String]("github.token")

  // Change from OFormat to Reads
  def get[Response](url: String)(implicit rds: Reads[Response], ec: ExecutionContext): EitherT[Future, APIError, Response] = {
    val request = ws.url(url)

    // Add Authorization header if the GitHub token is available
    val requestWithAuth = githubToken match {
      case Some(token) => request.addHttpHeaders("Authorization" -> s"token $token")
      case None => request
    }

    val response = requestWithAuth.get()

    EitherT {
      response
        .map { result =>
          result.status match {
            case 200 => Right(result.json.as[Response])
            case _ => Left(APIError.BadAPIResponse(result.status, result.statusText))
          }
        }
        .recover {
          case _: Throwable => Left(APIError.BadAPIResponse(500, "Could not connect"))
        }
    }
  }
}
