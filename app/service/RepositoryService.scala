package service

import cats.data.EitherT
import connector.GitHubConnector
import model.{APIError, Contents, Repository, User}
import play.api.libs.json.JsValue

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
@Singleton
class RepositoryService @Inject()(connector: GitHubConnector)(implicit ec: ExecutionContext) {

  def getGithubRepo(username: String): EitherT[Future, APIError, List[Repository]] = {
    val url = s"https://api.github.com/users/$username/repos"
    connector.get[List[Repository]](url)
  }

  def getRepoContents(username: String, repoName: String): EitherT[Future, APIError, List[Contents]] = {
    val url = s"https://api.github.com/repos/$username/$repoName/contents"
    connector.get[List[Contents]](url)
  }
  def getRepoFiles(username: String, repoName: String, path: String): EitherT[Future, APIError, List[Contents]] = {
    val url = s"https://api.github.com/repos/$username/$repoName/contents/$path"
    connector.get[List[Contents]](url)
  }

  def getFileContent(username: String, repoName: String, path: String): EitherT[Future, APIError, Contents] = {
    val url = s"https://api.github.com/repos/$username/$repoName/contents/$path"
    connector.get[Contents](url)
  }

  // New methods for creating, updating, and deleting files
  def createOrUpdateFile(username: String, repoName: String, path: String, message: String, content: String, sha: Option[String] = None): EitherT[Future, APIError, Contents] = {
    connector.createOrUpdateFile(username, repoName, path, message, content, sha)
  }

  def deleteFile(username: String, repoName: String, path: String, message: String, sha: String): EitherT[Future, APIError, Contents] = {
    connector.deleteFile(username, repoName, path, message, sha)
  }


}