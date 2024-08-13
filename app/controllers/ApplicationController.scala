package controllers

import model.{APIError, Repository, User}
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents, Request}
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import repository.DataRepository
import service.GitHubService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationController @Inject()(
                                       val controllerComponents: ControllerComponents,
                                       dataRepository: DataRepository,
                                       githubService: GitHubService
                                     )(implicit ec: ExecutionContext) extends BaseController with play.api.i18n.I18nSupport {

  // Fetch all users from the database
  def index(): Action[AnyContent] = Action.async { implicit request =>
    dataRepository.index().map {
      case Right(items) => Ok(Json.toJson(items))
      case Left(error) => Status(error.httpResponseStatus)(Json.toJson(error.reason))
    }
  }

  // Create a new user
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

  def getGitHubUser(username: String): Action[AnyContent] = Action.async { implicit request =>
    githubService.getGithubUser(username).value.map {
      case Right(user) =>
        Ok(views.html.gitHubUser(user))  // Renders the githubuser.scala.html template
      case Left(error) =>
        Redirect(routes.ApplicationController.index()).flashing("error" -> "User not found")
    }
  }

  def getGitHubRepo(username: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    githubService.getGithubRepo(username).value.map {
      case Right(repositories) => Ok(views.html.gitHubRepo(repositories, username)) // Pass both repositories and username
      case Left(APIError.BadAPIResponse(status, message)) => Status(status)(Json.obj("error" -> message))
    }
  }


  def addGitHubUser(username: String): Action[AnyContent] = Action.async { implicit request =>
    githubService.getGithubUser(username).value.flatMap {
      case Right(user) =>
        dataRepository.create(user).map {
          case Right(_) => Redirect(routes.ApplicationController.index) // Redirect to the index page after successful creation
          case Left(error) => InternalServerError(Json.toJson(error.reason))
        }
      case Left(error) => Future.successful(BadRequest(Json.obj("error" -> s"GitHub user not found: $username")))
    }
  }

  def getGitHubRepoContents(username: String, repoName: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    githubService.getRepoContents(username, repoName).value.map {
      case Right(contents) => Ok(Json.toJson(contents))
      case Left(APIError.BadAPIResponse(status, message)) => Status(status)(Json.obj("error" -> message))
    }
  }

  // Fetch a user by their login
  def read(login: String): Action[AnyContent] = Action.async { implicit request =>
    dataRepository.read(login).map {
      case Right(user) => Ok(Json.toJson(user))
      case Left(error) => Status(if (error.upstreamStatus == 404) NOT_FOUND else INTERNAL_SERVER_ERROR)(Json.toJson(error.reason))
    }
  }

  // Update an existing user
  def update(login: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[User] match {
      case JsSuccess(user, _) =>
        dataRepository.update(login, user).map {
          case Right(count) if count > 0 => Accepted
          case Right(_) => NotFound(Json.obj("error" -> "User not found"))  // No user updated, return 404
          case Left(error) if error.upstreamStatus == 404 => NotFound(Json.toJson(error.reason))  // Explicit 404 from repository
          case Left(error) => Status(error.httpResponseStatus)(Json.toJson(error.reason))
        }
      case JsError(errors) => Future.successful(BadRequest(Json.obj("errors" -> errors.toString)))
    }
  }

  // Delete a user by their login
  def delete(login: String): Action[AnyContent] = Action.async { implicit request =>
    if (login.trim.isEmpty) {
      Future.successful(BadRequest(Json.obj("error" -> "Invalid ID")))
    } else {
      dataRepository.delete(login).map {
        case Right(count) if count > 0 => Accepted
        case Right(_) => NotFound(Json.obj("error" -> "User not found"))  // No user deleted, return 404
        case Left(error) => Status(error.httpResponseStatus)(Json.toJson(error.reason))
      }
    }
  }

  def searchGitHubUser(): Action[AnyContent] = Action { implicit request =>
    val usernameOption = request.getQueryString("username")
    usernameOption match {
      case Some(username) => Redirect(routes.ApplicationController.getGitHubRepo(username))
      case None => BadRequest("Username not provided")
    }
  }

}
