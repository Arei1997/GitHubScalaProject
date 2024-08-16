package service

import cats.data.EitherT
import connector.GitHubConnector
import model.{APIError, Contents, CreateOrUpdate, Delete, Repository, User}
import play.api.libs.json.JsValue

import java.util.Base64
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
    val url = s"https://api.github.com/repos/$username/$repoName/contents/$path"
    val createOrUpdateData = CreateOrUpdate(
      message = message,
      content = Base64.getEncoder.encodeToString(content.getBytes("UTF-8")),
      sha = sha
    )
    connector.createOrUpdate[Contents](url, createOrUpdateData)
  }

  def deleteFile(username: String, repoName: String, path: String, message: String, sha: String): EitherT[Future, APIError, Contents] = {
    val url = s"https://api.github.com/repos/$username/$repoName/contents/$path"
    val deleteData = Delete(
      message = message,
      sha = sha
    )
    connector.delete[Contents](url, deleteData)
  }



}