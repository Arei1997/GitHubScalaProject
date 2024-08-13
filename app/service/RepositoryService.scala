package service

import cats.data.EitherT
import connector.{GitHubConnector, RepositoryConnector}
import model.{APIError, Repository, User}
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RepositoryService @Inject()(connector: RepositoryConnector)(implicit ec: ExecutionContext) {

  def getGithubRepo(username: String): EitherT[Future, APIError, List[Repository]] = {
    val url = s"https://api.github.com/users/$username/repos"
    connector.getRepositories(url)
  }
}

