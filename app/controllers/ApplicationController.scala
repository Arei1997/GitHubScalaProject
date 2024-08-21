package controllers

import model.{APIError, Repository, User}
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents, Request}
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import repository.DataRepository
import service.{GitHubService, RepositoryService}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationController @Inject()(
                                       val controllerComponents: ControllerComponents,
                                       dataRepository: DataRepository,
                                       githubService: GitHubService,
                                       repositoryService: RepositoryService
                                     )(implicit ec: ExecutionContext) extends BaseController with play.api.i18n.I18nSupport {

  // Fetch all users from the database
  def index(): Action[AnyContent] = Action.async { implicit request =>
    dataRepository.index().map {
      case Right(items) => Ok(Json.toJson(items))
      case Left(error) => Status(error.httpResponseStatus)(Json.toJson(error.reason))
    }
  }

  private def getGithubUserContributionGraphImage(username: String): String = {
    s"https://ghchart.rshah.org/$username"
  }

  def getGitHubUserContributionMap(username: String): Action[AnyContent] = Action { implicit request =>
    val contributionGraphImageUrl = getGithubUserContributionGraphImage(username)
    Ok(views.html.gitHubUserContributionMap(username, contributionGraphImageUrl))
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
    val contributionGraphImageUrl = s"https://ghchart.rshah.org/$username"

    githubService.getGithubUser(username).value.map {
      case Right(user) =>
        Ok(views.html.gitHubUser(user, contributionGraphImageUrl))  // Pass both user and contributionGraphImageUrl
      case Left(error) =>
        Redirect(routes.ApplicationController.index()).flashing("error" -> "User not found")
    }
  }

  def getGitHubRepo(username: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    repositoryService.getGithubRepo(username).value.map {
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

  def getGitHubRepoContents(username: String, repoName: String): Action[AnyContent] = Action.async { implicit request =>
    repositoryService.getRepoContents(username, repoName).value.map {
      case Right(contents) =>
        if (request.headers.get("Accept").contains("application/json")) {
          Ok(Json.toJson(contents)) // Return JSON if the request is expecting JSON
        } else {
          Ok(views.html.gitHubRepoContents(username, repoName, contents)) // Render HTML otherwise
        }
      case Left(APIError.BadAPIResponse(status, message)) =>
        Status(status)(Json.obj("error" -> message))
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
    dataRepository.delete(login).map {
      case Right(deleteResult) =>
        if (deleteResult.getDeletedCount > 0) {
          Accepted(Json.toJson("Item successfully deleted"))
        } else {
          NotFound(Json.toJson("Item not found"))
        }
      case Left(apiError) =>
        Status(apiError.httpResponseStatus)(Json.toJson(apiError.reason))
    }
  }

  def searchGitHubUser(): Action[AnyContent] = Action { implicit request =>
    val usernameOption = request.getQueryString("username")
    usernameOption match {
      case Some(username) => Redirect(routes.ApplicationController.getGitHubRepo(username))
      case None => BadRequest("Username not provided")
    }
  }

  def getGitHubFile(username: String, repoName: String, path: String): Action[AnyContent] = Action.async { implicit request =>
    repositoryService.getFileContent(username, repoName, path).value.flatMap {
      case Right(file) if file.content.isDefined =>
        try {
          val contentWithoutNewlines = file.content.get.replaceAll("\\s", "")
          val decodedContent = new String(java.util.Base64.getDecoder.decode(contentWithoutNewlines), "UTF-8")
          Future.successful(Ok(views.html.gitHubFileContents(username, repoName, path, decodedContent, file.sha)))
        } catch {
          case _: IllegalArgumentException =>
            Future.successful(BadRequest("Invalid base64 content"))
        }

      case Right(_) =>
        Future.successful(NotFound(s"No file content found at $path"))

      case Left(APIError.BadAPIResponse(status, message)) =>
        Future.successful(Status(status)(s"Error fetching file content: $message"))
    }
  }


  def getGitHubFolder(username: String, repoName: String, path: String): Action[AnyContent] = Action.async { implicit request =>
    repositoryService.getRepoFiles(username, repoName, path).value.map {
      case Right(contents) => Ok(views.html.gitHubRepoContents(username, repoName, contents))
      case Left(APIError.BadAPIResponse(status, message)) => Status(status)(Json.obj("error" -> message))
    }.recover {
      case ex: Exception => InternalServerError(Json.obj("error" -> ex.getMessage))
    }
  }










}
