package service

import cats.data.EitherT
import connector.GitHubConnector
import model.{APIError, Repository, User}
import play.api.libs.json.JsValue



import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
@Singleton
class RepositoryService @Inject()(connector: GitHubConnector)(implicit ec: ExecutionContext) {

  def getGithubRepo(username: String): EitherT[Future, APIError, List[Repository]] = {
    val url = s"https://api.github.com/users/$username/repos"
    connector.get[List[Repository]](url)
  }

  def getRepoContents(username: String, repoName: String): EitherT[Future, APIError, JsValue] = {
    val url = s"https://api.github.com/repos/$username/$repoName/contents"
    connector.get[JsValue](url)
  }
}