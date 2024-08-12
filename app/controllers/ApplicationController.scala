package controllers

import model.{APIError, User}
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents, Request}
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import repository.DataRepository
import service.GitHubService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import views.html.gitHubUser  // Import the GitHub user template

@Singleton
class ApplicationController @Inject()(
                                       val controllerComponents: ControllerComponents,
                                       dataRepository: DataRepository,
                                       githubService: GitHubService
                                     )(implicit ec: ExecutionContext) extends BaseController with play.api.i18n.I18nSupport {

  def index(): Action[AnyContent] = Action.async { implicit request =>
    dataRepository.index().map {
      case Right(items) => Ok(Json.toJson(items))
      case Left(error) => Status(error.httpResponseStatus)(Json.toJson(error.reason))
    }
  }

  def create(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[User] match {
      case JsSuccess(user, _) =>
        dataRepository.create(user).map {
          case Right(createdUser) => Created(Json.toJson(createdUser))
          case Left(error) => Status(error.httpResponseStatus)(Json.toJson(error.reason))
        }
      case JsError(errors) => Future.successful(BadRequest(Json.obj("errors" -> errors.toString)))
    }
  }

  def getGitHubUser(username: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    githubService.getGithubUser(username).value.map {
      case Right(user) => Ok(gitHubUser(user))  // Use the imported GitHub user template to render the view
      case Left(APIError.BadAPIResponse(status, message)) => Status(status)(Json.obj("error" -> message))
    }
  }

  def read(login: String): Action[AnyContent] = Action.async { implicit request =>
    dataRepository.read(login).map {
      case Right(user) => Ok(Json.toJson(user))
      case Left(error) => Status(if (error.upstreamStatus == 404) NOT_FOUND else INTERNAL_SERVER_ERROR)(Json.toJson(error.reason))
    }
  }

  def update(login: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[User] match {
      case JsSuccess(user, _) =>
        dataRepository.update(login, user).map {
          case Right(count) if count > 0 => Accepted
          case Right(_) => NotFound  // No user updated, return 404
          case Left(error) if error.upstreamStatus == 404 => NotFound(Json.toJson(error.reason))  // Explicit 404 from repository
          case Left(error) => Status(error.httpResponseStatus)(Json.toJson(error.reason))
        }
      case JsError(errors) => Future.successful(BadRequest(Json.obj("errors" -> errors.toString)))
    }
  }

  def delete(login: String): Action[AnyContent] = Action.async { implicit request =>
    if (login.trim.isEmpty) {
      Future.successful(BadRequest(Json.obj("error" -> "Invalid ID")))
    } else {
      dataRepository.delete(login).map {
        case Right(count) if count > 0 => Accepted
        case Right(_) => NotFound  // No user deleted, return 404
        case Left(error) => Status(error.httpResponseStatus)(Json.toJson(error.reason))
      }
    }
  }
}
