package controllers

import javax.inject._
import play.api.mvc._
import service.NewsService
import scala.concurrent.ExecutionContext

@Singleton
class NewsController @Inject()(
                                val controllerComponents: ControllerComponents,
                                newsService: NewsService
                              )(implicit ec: ExecutionContext) extends BaseController {

  def index(category: Option[String]): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    val newsFuture = category match {
      case Some(cat) => newsService.fetchNewsByCategory(cat)
      case None => newsService.fetchTechNews()
    }

    newsFuture.map { articles =>
      Ok(views.html.news(articles, category))
    }
  }
}
