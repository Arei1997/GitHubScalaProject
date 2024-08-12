package service

import cats.data.EitherT
import connector.GitHubConnector
import model.{APIError, User}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GitHubService @Inject()(connector: GitHubConnector) {

  def getGithubUser(username: String, urlOverride: Option[String] = None)
                   (implicit ec: ExecutionContext): EitherT[Future, APIError, User] = {
    val url = urlOverride.getOrElse(s"https://api.github.com/users/$username")

    connector.get[User](url).leftMap {
      case APIError.BadAPIResponse(status, message) => APIError.BadAPIResponse(status, message)
      case otherError => APIError.BadAPIResponse(500, otherError.toString)
    }
  }
}
