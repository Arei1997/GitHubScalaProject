//package service
//
//import cats.data.EitherT
//import model.{APIError, User}
//import org.mongodb.scala.result.{DeleteResult, UpdateResult}
//import repository.DataRepository
//
//import scala.concurrent.{ExecutionContext, Future}
//import javax.inject.Inject
//
//
//class RepositoryService @Inject()(dataRepository: DataRepository)(implicit ec: ExecutionContext) {
//
//    def readAll(): Future[Either[APIError, Seq[User]]] = {
//      dataRepository.index().map {
//        case Right(items) => Right(items)
//        case Left(error) => Left(error)
//      }
//    }
//
//    def create(dataModel: User): Future[Either[APIError, User]] = {
//      dataRepository.create(dataModel)
//    }
//
//    def read(login: String): Future[Either[APIError, Option[User]]] = {
//      dataRepository.read(login)
//    }
//
//    def update(id: String, book: User): Future[Either[APIError, UpdateResult]] =
//      dataRepository.update(id, book)
//
//    def delete(id: String): Future[Either[APIError, DeleteResult]] = {
//      dataRepository.delete(id)
//    }
//
//  }