package controllers

import model.{APIError, User}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{ControllerComponents, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import repository.DataRepository
import service.{GitHubService, RepositoryService}

import scala.concurrent.{ExecutionContext, Future}

class ApplicationControllerSpec extends AnyWordSpec with Matchers with MockitoSugar with ScalaFutures with BeforeAndAfterEach {

  // Mock dependencies
  val mockRepositoryService: RepositoryService = mock[RepositoryService]
  val mockGitHubService: GitHubService = mock[GitHubService]
  val mockDataRepository: DataRepository = mock[DataRepository]
  val mockControllerComponents: ControllerComponents = Helpers.stubControllerComponents()

  // Controller instance with injected mocks
  val testController = new ApplicationController(
    mockControllerComponents,
    mockDataRepository,
    mockGitHubService,
    mockRepositoryService
  )(ExecutionContext.global)

  override def beforeEach(): Unit = {
    // Reset mocks before each test
    reset(mockRepositoryService, mockGitHubService, mockDataRepository)
  }

  private val testUser: User = User(
    login = "johndoe",
    name = Some("John Doe"),
    avatar_url = "http://example.com/avatar.jpg",
    location = Some("New York, USA"),
    bio = Some("Software developer and open source enthusiast."),
    followers = 150,
    following = 100,
    created_at = "2023-01-01T12:00:00Z"
  )

  "ApplicationController .create" should {

    "create a user in the database" in {
      when(mockDataRepository.create(any[User])).thenReturn(Future.successful(Right(testUser)))

      val request: FakeRequest[JsValue] = FakeRequest(POST, "/api").withBody(Json.toJson(testUser))
      val result: Future[Result] = testController.create()(request)

      status(result) shouldBe CREATED
      contentAsJson(result) shouldBe Json.toJson(testUser)
    }

    "return a BadRequest for invalid JSON" in {
      val invalidRequest: FakeRequest[JsValue] = FakeRequest(POST, "/api").withBody(Json.obj("invalid" -> "data"))
      val result: Future[Result] = testController.create()(invalidRequest)

      status(result) shouldBe BAD_REQUEST
      contentAsJson(result) should include("errors")
    }
  }

  "ApplicationController .read" should {

    "find a user in the database by id" in {
      when(mockDataRepository.read("johndoe")).thenReturn(Future.successful(Right(testUser)))

      val result: Future[Result] = testController.read("johndoe")(FakeRequest())

      status(result) shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(testUser)
    }

    "return NotFound if the user is not found" in {
      when(mockDataRepository.read("nonexistent_id")).thenReturn(Future.successful(Left(APIError.BadAPIResponse(404, "User not found"))))

      val result: Future[Result] = testController.read("nonexistent_id")(FakeRequest())

      status(result) shouldBe NOT_FOUND
    }
  }

  "ApplicationController .update" should {

    "update a user’s number of followers in the database" in {
      val updatedUser = testUser.copy(followers = 200)
      when(mockDataRepository.update(eqTo("johndoe"), any[User])).thenReturn(Future.successful(Right(1)))

      val updateRequest: FakeRequest[JsValue] = FakeRequest(PUT, s"/api/johndoe").withBody(Json.toJson(updatedUser))
      val result: Future[Result] = testController.update("johndoe")(updateRequest)

      status(result) shouldBe ACCEPTED
    }

    "return NotFound if the user to update is not found" in {
      when(mockDataRepository.update(eqTo("nonexistent_id"), any[User])).thenReturn(Future.successful(Left(APIError.BadAPIResponse(404, "User not found"))))

      val updatedUser = testUser.copy(login = "UpdatedLogin")
      val updateRequest: FakeRequest[JsValue] = FakeRequest(PUT, s"/api/nonexistent_id").withBody(Json.toJson(updatedUser))
      val result: Future[Result] = testController.update("nonexistent_id")(updateRequest)

      status(result) shouldBe NOT_FOUND
    }

    "return BadRequest for invalid JSON" in {
      val invalidRequest: FakeRequest[JsValue] = FakeRequest(PUT, s"/api/johndoe").withBody(Json.obj("invalid" -> "data"))
      val result: Future[Result] = testController.update("johndoe")(invalidRequest)

      status(result) shouldBe BAD_REQUEST
    }
  }

  "ApplicationController .delete" should {

    "delete a user in the database" in {
      when(mockDataRepository.delete("johndoe")).thenReturn(Future.successful(Right(new com.mongodb.client.result.DeleteResult {
        override def wasAcknowledged(): Boolean = true
        override def getDeletedCount: Long = 1
      })))

      val result: Future[Result] = testController.delete("johndoe")(FakeRequest())

      status(result) shouldBe ACCEPTED
    }

    "return NotFound if the user to delete is not found" in {
      when(mockDataRepository.delete("nonexistent_id")).thenReturn(Future.successful(Right(new com.mongodb.client.result.DeleteResult {
        override def wasAcknowledged(): Boolean = true
        override def getDeletedCount: Long = 0
      })))

      val result: Future[Result] = testController.delete("nonexistent_id")(FakeRequest())

      status(result) shouldBe NOT_FOUND
    }
  }
}
