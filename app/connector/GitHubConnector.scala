package connector

import cats.data.EitherT
import model.{APIError, Contents}
import play.api.libs.json.{JsObject, Json, Reads}
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import play.api.Configuration

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import java.util.Base64

class GitHubConnector @Inject()(ws: WSClient, config: Configuration)(implicit ec: ExecutionContext) {

  private val githubToken: Option[String] = config.getOptional[String]("github.token")

  private def addAuthorizationHeader(request: WSRequest): WSRequest = {
    githubToken match {
      case Some(token) => request.addHttpHeaders("Authorization" -> s"token $token")
      case None => request
    }
  }

  def get[T](url: String)(implicit rds: Reads[T], ec: ExecutionContext): EitherT[Future, APIError, T] = {
    val request = ws.url(url)
    val requestWithAuth = addAuthorizationHeader(request)
    EitherT {
      requestWithAuth.get().map { response =>
        response.status match {
          case 200 => Right(response.json.as[T])
          case _ => Left(APIError.BadAPIResponse(response.status, response.statusText))
        }
      }.recover {
        case ex: Exception => Left(APIError.BadAPIResponse(500, ex.getMessage))
      }
    }
  }

  def createOrUpdateFile(username: String, repoName: String, path: String, message: String, content: String, sha: Option[String]): EitherT[Future, APIError, Contents] = {
    val url = s"https://api.github.com/repos/$username/$repoName/contents/$path"
    val requestBody = Json.obj(
      "message" -> message,
      "content" -> Base64.getEncoder.encodeToString(content.getBytes("UTF-8")),
      "sha" -> sha
    )
    put[Contents](url, requestBody)
  }

  def deleteFile(username: String, repoName: String, path: String, message: String, sha: String): EitherT[Future, APIError, Contents] = {
    val url = s"https://api.github.com/repos/$username/$repoName/contents/$path"
    val requestWithAuth = addAuthorizationHeader(ws.url(url))
      .withQueryStringParameters(
        "message" -> message,
        "sha" -> sha
      )

    EitherT {
      requestWithAuth.delete().map(handleResponse[Contents])
    }
  }

  private def put[T](url: String, body: JsObject)(implicit rds: Reads[T], ec: ExecutionContext): EitherT[Future, APIError, T] = {
    val request = ws.url(url).withHttpHeaders("Content-Type" -> "application/json")
    val requestWithAuth = addAuthorizationHeader(request)
    EitherT {
      requestWithAuth.put(body).map(handleResponse[T])
    }
  }

  private def handleResponse[T](response: WSResponse)(implicit rds: Reads[T]): Either[APIError, T] = {
    response.status match {
      case 200 | 201 => Right(response.json.as[T])  // Handle successful JSON responses
      case 204 => Right(Json.obj().as[T])           // Handle 204 No Content by returning an empty JSON object
      case 409 => Left(APIError.BadAPIResponse(409, "Conflict: Possible SHA mismatch or file has been modified"))
      case _ => Left(APIError.BadAPIResponse(response.status, response.statusText))
    }
  }
}
