package service

import cats.data.EitherT
import com.sun.jdi.connect.Connector
import connector.GitHubConnector
import model.{APIError, Commit, Contents, CreateOrUpdate, Delete, Repository, User}
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

  def createOrUpdateFile(username: String, repoName: String, path: String, message: String, content: String, sha: Option[String] = None): EitherT[Future, APIError, Contents] = {
    val url = s"https://api.github.com/repos/$username/$repoName/contents/$path"

    val shaFuture: Future[Option[String]] = sha match {
      case Some(providedSha) => Future.successful(Some(providedSha))
      case None =>
        // Fetch the SHA key if not provided
        getFileContent(username, repoName, path).value.map {
          case Right(file) => file.sha: Option[String] // Explicitly cast to Option[String]
          case Left(_) => None
        }
    }

    EitherT {
      shaFuture.flatMap { fetchedSha =>
        val createOrUpdateData = CreateOrUpdate(
          message = message,
          content = Base64.getEncoder.encodeToString(content.getBytes("UTF-8")),
          sha = fetchedSha // fetchedSha is now correctly typed as Option[String]
        )
        connector.createOrUpdate[Contents](url, createOrUpdateData).value
      }
    }
  }




  def deleteFile(username: String, repoName: String, path: String, message: String, sha: String): EitherT[Future, APIError, Contents] = {
    val url = s"https://api.github.com/repos/$username/$repoName/contents/$path"
    val deleteData = Delete(
      message = message,
      sha = sha
    )
    connector.delete[Contents](url, deleteData)
  }

  def getRepoLanguages(username: String, repoName: String): EitherT[Future, APIError, Map[String, Int]] = {
    val url = s"https://api.github.com/repos/$username/$repoName/languages"
    connector.get[Map[String, Int]](url)
  }


  def getRepoLanguagesWithPercentage(username: String, repoName: String): EitherT[Future, APIError, Map[String, Double]] = {
    getRepoLanguages(username, repoName).map { languages =>
      val totalSize = languages.values.sum.toDouble
      languages.map { case (language, size) =>
        language -> (size / totalSize) * 100
      }
    }
  }



  def getCommitHistory(username: String, repoName: String): EitherT[Future, APIError, Seq[Commit]] = {
    connector.getCommits(username, repoName)
  }




}