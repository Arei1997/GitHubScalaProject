package service

import cats.data.EitherT
import connector.GitHubConnector
import model.{APIError, Repository, User}
import play.api.libs.json.JsValue

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GitHubService @Inject()(connector: GitHubConnector)(implicit ec: ExecutionContext) {

  def getGithubUser(username: String, urlOverride: Option[String] = None): EitherT[Future, APIError, User] = {
    val url = urlOverride.getOrElse(s"https://api.github.com/users/$username")
    connector.get[User](url).leftMap {
      case APIError.BadAPIResponse(status, message) =>
        println(s"Failed to fetch user from GitHub: $message")
        APIError.BadAPIResponse(status, s"Failed to fetch user from GitHub: $message")
      case otherError =>
        println(s"An unexpected error occurred: $otherError")
        APIError.BadAPIResponse(500, s"An unexpected error occurred: $otherError")
    }
  }

  def getTopScalaRepositories(): EitherT[Future, APIError, Seq[Repository]] = {
    val url = "https://api.github.com/search/repositories?q=language:scala&sort=stars&order=desc&per_page=6"
    connector.get[JsValue](url).map { json =>
      (json \ "items").as[Seq[Repository]]
    }
  }

}
