package controllers

import model.{APIError, Contents, Delete, FileFormData}
import play.api.i18n.Messages
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, Request}
import service.RepositoryService
import views.html.helper.CSRF

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GitHubReposController @Inject()(repositoryService: RepositoryService, cc: ControllerComponents)(implicit ec: ExecutionContext) extends AbstractController(cc) with play.api.i18n.I18nSupport {

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

  def createFileForm(username: String, repoName: String, path: String): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.fileForm(FileFormData.form, username, repoName, path))
  }

  def submitFileForm(username: String, repoName: String, path: String): Action[AnyContent] = Action.async { implicit request =>
    FileFormData.form.bindFromRequest().fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.fileForm(formWithErrors, username, repoName, path)))
      },
      data => {
        val fileName = if (path.isEmpty || path == "root") {
          data.fileName
        } else {
          path
        }

        repositoryService.getFileContent(username, repoName, fileName).value.flatMap {
          case Right(existingFile) =>
            // Update the file if it exists
            repositoryService.createOrUpdateFile(username, repoName, fileName, data.message, data.content, Some(existingFile.sha)).value.map {
              case Right(_) => Ok(views.html.fileForm(FileFormData.form.fill(data), username, repoName, fileName)).flashing("success" -> "File updated successfully")
              case Left(error) => BadRequest(views.html.fileForm(FileFormData.form.withError("error", error.reason), username, repoName, fileName))
            }
          case Left(_) =>
            // Create a new file if it doesn't exist
            repositoryService.createOrUpdateFile(username, repoName, fileName, data.message, data.content, None).value.map {
              case Right(_) => Ok(views.html.fileForm(FileFormData.form.fill(data), username, repoName, fileName)).flashing("success" -> "File created successfully")
              case Left(error) => BadRequest(views.html.fileForm(FileFormData.form.withError("error", error.reason), username, repoName, fileName))
            }
        }
      }
    )
  }

  def deleteFileForm(username: String, repoName: String, path: String): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.deleteFileForm(Delete.form, username, repoName, path))
  }

  def submitDeleteFileForm(username: String, repoName: String, path: String): Action[AnyContent] = Action.async { implicit request =>
    Delete.form.bindFromRequest().fold(
      formWithErrors => {
        Future.successful(BadRequest(views.html.deleteFileForm(formWithErrors, username, repoName, path)))
      },
      data => {
        repositoryService.deleteFile(username, repoName, path, data.message, data.sha).value.map {
          case Right(_) => Redirect(routes.GitHubReposController.deleteFileForm(username, repoName, path)).flashing("success" -> "File deleted successfully")
          case Left(error) => BadRequest(views.html.deleteFileForm(Delete.form.withError("error", error.reason), username, repoName, path))
        }
      }
    )
  }
}
