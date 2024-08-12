package service

import cats.data.EitherT
import connector.GitHubConnector
import model.{APIError, User}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GitHubService @Inject()(connector: GitHubConnector)(implicit ec: ExecutionContext) {

  def getGithubUser(username: String, urlOverride: Option[String] = None): EitherT[Future, APIError, User] = {
    // Use the overridden URL if provided, otherwise default to the GitHub API URL
    val url = urlOverride.getOrElse(s"https://api.github.com/users/$username")

    // Make the request through the connector and handle the response
    connector.get[User](url).leftMap {
      case APIError.BadAPIResponse(status, message) =>
        // Return the specific API error with the status and message
        APIError.BadAPIResponse(status, s"Failed to fetch user from GitHub: $message")
      case otherError =>
        // Handle any other errors that might occur
        APIError.BadAPIResponse(500, s"An unexpected error occurred: $otherError")
    }
  }
}
