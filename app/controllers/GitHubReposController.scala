package controllers

import model.{APIError, Repository, User}
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents, Request}
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import repository.DataRepository
import service.{GitHubService, RepositoryService}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GitHubReposController @Inject()(
                                       val controllerComponents: ControllerComponents,
                                       dataRepository: DataRepository,
                                       githubService: GitHubService,
                                       repositoryService: RepositoryService
                                     )(implicit ec: ExecutionContext) extends BaseController with play.api.i18n.I18nSupport {



}