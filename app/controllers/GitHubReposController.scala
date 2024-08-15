package controllers

import model.{APIError, Contents}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import service.RepositoryService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GitHubReposController @Inject()(repositoryService: RepositoryService, cc: ControllerComponents)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def readFile(username: String, repoName: String, path: String): Action[AnyContent] = Action.async { implicit request =>
    repositoryService.getFileContent(username, repoName, path).value.map {
      case Right(fileContent) =>
        Ok(Json.toJson(fileContent))
      case Left(error) =>
        error match {
          case APIError.BadAPIResponse(status, message) =>
            Status(status)(Json.obj("error" -> message))
        }
    }
  }
}
