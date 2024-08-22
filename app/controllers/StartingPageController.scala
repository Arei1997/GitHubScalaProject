package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import service.GitHubService
import scala.concurrent.ExecutionContext

@Singleton
class StartingPageController @Inject()(
                                val controllerComponents: ControllerComponents,
                                val gitHubService: GitHubService
                              )(implicit ec: ExecutionContext) extends BaseController {

  def startingPage: Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    gitHubService.getTopScalaRepositories().value.map {
      case Right(repositories) => Ok(views.html.startingPage(repositories))
      case Left(error) => InternalServerError(views.html.errorPage(s"An error occurred: ${error.toString}"))
    }
  }
}
