package controllers

import model.{APIError, Contents}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import service.RepositoryService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GitHubReposController @Inject()(repositoryService: RepositoryService, cc: ControllerComponents)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def readFile(username: String, repoName: String, path: String): Action[AnyContent] = Action.async { implicit request =>
    repositoryService.getFileContent(username, repoName, path).value.map {
      case Right(fileContent) => Ok(Json.toJson(fileContent))
      case Left(error) => Status(error.httpResponseStatus)(Json.obj("error" -> error.reason))
    }
  }

  def createOrUpdateFile(username: String, repoName: String, path: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    val message = (request.body \ "message").as[String]
    val content = (request.body \ "content").as[String]
    val sha = (request.body \ "sha").asOpt[String]

    repositoryService.createOrUpdateFile(username, repoName, path, message, content, sha).value.map {
      case Right(file) => Ok(Json.toJson(file))
      case Left(error) => Status(error.httpResponseStatus)(Json.obj("error" -> error.reason))
    }
  }

  def deleteFile(username: String, repoName: String, path: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    val message = (request.body \ "message").as[String]
    val sha = (request.body \ "sha").as[String]

    repositoryService.deleteFile(username, repoName, path, message, sha).value.map {
      case Right(response) => Ok(Json.toJson(response))
      case Left(error) => Status(error.httpResponseStatus)(Json.obj("error" -> error.reason))
    }
  }
}

