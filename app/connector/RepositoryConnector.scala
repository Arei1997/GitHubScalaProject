package connector

import cats.data.EitherT
import model.{APIError, Repository}
import play.api.libs.json.{Json, OFormat}
import play.api.libs.ws.{WSClient, WSResponse}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RepositoryConnector @Inject()(ws: WSClient)(implicit ec: ExecutionContext) {

  def getRepositories(url: String): EitherT[Future, APIError, List[Repository]] = {
    val request = ws.url(url)

    EitherT {
      request.get().map { response =>
        response.status match {
          case 200 =>
            response.json.validate[List[Repository]].asEither.left.map { errors =>
              APIError.BadAPIResponse(500, s"JSON parsing error: ${errors.mkString(", ")}")
            }
          case _ =>
            Left(APIError.BadAPIResponse(response.status, response.statusText))
        }
      }.recover {
        case ex: Exception =>
          Left(APIError.BadAPIResponse(500, s"Could not connect: ${ex.getMessage}"))
      }
    }
  }
}
