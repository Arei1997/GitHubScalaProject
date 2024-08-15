package controllers

import cats.data.EitherT
import connector.GitHubConnector
import model.APIError
import play.api.mvc.{AbstractController, Action, AnyContent, BaseController, ControllerComponents, Request}
import play.api.libs.json.JsValue
import model.Contents
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GitHubReposController @Inject()(gitHubConnector: GitHubConnector, cc: ControllerComponents)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def readFile(username: String, repoName: String, path: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    val url = s"https://api.github.com/repos/$username/$repoName/contents/$path"

    val response: EitherT[Future, APIError, JsValue] = gitHubConnector.get[Contents](url)



  }