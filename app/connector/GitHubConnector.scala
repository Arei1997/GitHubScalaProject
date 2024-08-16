package connector

import cats.data.EitherT
import com.google.inject.Inject
import com.typesafe.config.ConfigFactory
import model.{APIError, Contents, CreateOrUpdate, Delete}
import play.api.libs.json.{Json, Reads}
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.{ExecutionContext, Future}

class GitHubConnector @Inject()(ws: WSClient)(implicit ec: ExecutionContext) {

  // Load the personal access token from the configuration
  private val personalAccessToken = ConfigFactory.load().getString("github.token")

  def get[Response](url: String)(implicit rds: Reads[Response], ec: ExecutionContext): EitherT[Future, APIError, Response] = {
    val request = ws.url(url).addHttpHeaders(
      "Accept" -> "application/vnd.github+json",
      "Authorization" -> s"Bearer $personalAccessToken"
    )

    val response = request.get()

    EitherT {
      response.map { result =>
        if (result.status == 200) {
          Right(result.json.as[Response])
        } else {
          Left(APIError.BadAPIResponse(result.status, result.statusText))
        }
      }.recover { case _: Throwable =>
        Left(APIError.BadAPIResponse(500, "Could not connect to API."))
      }
    }
  }

  def createOrUpdate[Response](url: String, data: CreateOrUpdate)(implicit rds: Reads[Response], ec: ExecutionContext): EitherT[Future, APIError, Response] = {
    val request = ws.url(url).addHttpHeaders(
      "Accept" -> "application/vnd.github+json",
      "Authorization" -> s"Bearer $personalAccessToken"
    )

    val response = request.put(Json.toJson(data))

    EitherT {
      response.map { result =>
        if (result.status == 200 || result.status == 201) {
          Right(result.json.as[Response])
        } else {
          Left(APIError.BadAPIResponse(result.status, result.statusText))
        }
      }.recover { case _: Throwable =>
        Left(APIError.BadAPIResponse(500, "Could not connect to API."))
      }
    }
  }

  def delete[Response](url: String, data: Delete)(implicit rds: Reads[Response], ec: ExecutionContext): EitherT[Future, APIError, Response] = {
    val request = ws.url(url).addHttpHeaders(
      "Accept" -> "application/vnd.github+json",
      "Authorization" -> s"Bearer $personalAccessToken"
    )

    val response = request.withMethod("DELETE").withBody(Json.toJson(data)).execute()

    EitherT {
      response.map { result =>
        if (result.status == 200) {
          Right(result.json.as[Response])
        } else {
          Left(APIError.BadAPIResponse(result.status, result.statusText))
        }
      }.recover { case _: Throwable =>
        Left(APIError.BadAPIResponse(500, "Could not connect to API."))
      }
    }
  }
}
