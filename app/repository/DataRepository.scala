package repository

import model.{APIError, User}
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model._
import play.api.libs.json.JsValue
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
//hello
@Singleton
class DataRepository @Inject() (
                                 mongoComponent: MongoComponent
                               )(implicit ec: ExecutionContext) extends PlayMongoRepository[User](
  collectionName = "Users",
  mongoComponent = mongoComponent,
  domainFormat = User.format, // Ensure this is correctly referenced
  indexes = Seq(IndexModel(
    Indexes.ascending("login")
  )),
  replaceIndexes = false
) {

  def index(): Future[Either[APIError.BadAPIResponse, Seq[User]]] =
    collection.find().toFuture().map { users =>
      if (users.nonEmpty) Right(users)
      else Left(APIError.BadAPIResponse(404, "Users cannot be found"))
    }.recover {
      case e: Throwable => Left(APIError.BadAPIResponse(500, e.getMessage))
    }

  def create(user: User): Future[Either[APIError.BadAPIResponse, User]] =
    collection.insertOne(user).toFuture().map { _ =>
      Right(user)
    }.recover {
      case e: Throwable => Left(APIError.BadAPIResponse(500, e.getMessage))
    }

  private def byID(login: String): Bson =
    Filters.equal("login", login)

  def read(login: String): Future[Either[APIError.BadAPIResponse, User]] = {
    collection.find(byID(login)).headOption().map {
      case Some(user) => Right(user)
      case None => Left(APIError.BadAPIResponse(404, "User not found"))
    }.recover {
      case e: Throwable => Left(APIError.BadAPIResponse(500, e.getMessage))
    }
  }

  def update(login: String, user: User): Future[Either[APIError.BadAPIResponse, Long]] =
    collection.replaceOne(
      filter = byID(login),
      replacement = user,
      options = new ReplaceOptions().upsert(false) // upsert set to false to avoid creation if not found
    ).toFuture().map { updateResult =>
      if (updateResult.getModifiedCount > 0) Right(updateResult.getModifiedCount)
      else Left(APIError.BadAPIResponse(404, "User not found or not modified"))
    }.recover {
      case e: Throwable => Left(APIError.BadAPIResponse(500, e.getMessage))
    }

  def delete(login: String): Future[Either[APIError.BadAPIResponse, Long]] =
    collection.deleteOne(byID(login)).toFuture().map { deleteResult =>
      if (deleteResult.getDeletedCount > 0) Right(deleteResult.getDeletedCount)
      else Left(APIError.BadAPIResponse(404, "User not found"))
    }.recover {
      case e: Throwable => Left(APIError.BadAPIResponse(500, e.getMessage))
    }

  def deleteAll(): Future[Unit] =
    collection.deleteMany(empty()).toFuture().map(_ => ()) // Needed for tests

}
