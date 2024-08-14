package controllers
// bilal's comment
import baseSpec.BaseSpecWithApplication
import model.User
import play.api.test.FakeRequest
import play.api.http.Status
import play.api.test.Helpers._
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContent, BaseController, ControllerComponents, Result}
import repository._
import service.GitHubService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ApplicationControllerSpec extends BaseSpecWithApplication {

  val TestApplicationController = new ApplicationController(

    component,
    repository,
    service,
    repoService
  )(executionContext)

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
      beforeEach()

      val request: FakeRequest[JsValue] = buildPost("/api").withBody(Json.toJson(testUser))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      status(createdResult) shouldBe Status.CREATED

      afterEach()
    }

    "return a BadRequest for invalid JSON" in {
      val invalidRequest: FakeRequest[JsValue] = buildPost("/api").withBody(Json.obj("invalid" -> "data"))
      val result: Future[Result] = TestApplicationController.create()(invalidRequest)

      status(result) shouldBe Status.BAD_REQUEST
    }
  }


  "ApplicationController .read" should {

    "find a book in the database by id" in {
      beforeEach()

      val request: FakeRequest[JsValue] = buildGet(s"/api/${testUser.login}").withBody(Json.toJson(testUser))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      status(createdResult) shouldBe Status.CREATED

      val readResult: Future[Result] = TestApplicationController.read(testUser.login)(FakeRequest())

      status(readResult) shouldBe OK
      contentAsJson(readResult) shouldBe Json.toJson(testUser)

      afterEach()
    }
    "return NotFound if the book is not found" in {
      val readResult: Future[Result] = TestApplicationController.read("nonexistent_id")(FakeRequest())

      status(readResult) shouldBe Status.NOT_FOUND
    }
  }

  "ApplicationController .update" should {
    "update a user’s number of followers in the database" in {
      beforeEach()

      val request: FakeRequest[JsValue] = FakeRequest(POST, "/api").withBody(Json.toJson(testUser))
      val createdResult: Future[Result] = TestApplicationController.create()(request)
      status(createdResult) shouldBe Status.CREATED

      val updatedUser: User = testUser.copy(followers = 200) // Updated number of followers
      val updatedRequest: FakeRequest[JsValue] = FakeRequest(PUT, s"/api/${testUser.login}").withBody(Json.toJson(updatedUser))
      val updatedResult: Future[Result] = TestApplicationController.update(testUser.login)(updatedRequest)

      status(updatedResult) shouldBe ACCEPTED

      val readResult: Future[Result] = TestApplicationController.read(testUser.login)(FakeRequest(GET, s"/api/${testUser.login}"))
      status(readResult) shouldBe OK
      contentAsJson(readResult) shouldBe Json.toJson(updatedUser)

      afterEach()
    }

    "return NotFound if the user to update is not found" in {
      val updatedUser: User = testUser.copy(login = "UpdatedLogin")
      val updatedRequest: FakeRequest[JsValue] = buildPut(s"/api/nonexistent_id").withBody(Json.toJson(updatedUser))
      val updatedResult: Future[Result] = TestApplicationController.update("nonexistent_id")(updatedRequest)

      status(updatedResult) shouldBe Status.NOT_FOUND
    }

    "return BadRequest for invalid JSON" in {
      val invalidRequest: FakeRequest[JsValue] = buildPut(s"/api/${testUser.login}").withBody(Json.obj("invalid" -> "data"))
      val result: Future[Result] = TestApplicationController.update(testUser.login)(invalidRequest)

      status(result) shouldBe Status.BAD_REQUEST
    }
  }


  "ApplicationController .delete" should {
    "delete a user in the database" in {
      beforeEach()
      val request: FakeRequest[JsValue] = buildPost("/api").withBody(Json.toJson(testUser))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      status(createdResult) shouldBe CREATED

      val deleteRequest: FakeRequest[AnyContent] = buildDelete(s"/api/${testUser.login}")
      val deletedResult: Future[Result] = TestApplicationController.delete(testUser.login)(deleteRequest)

      status(deletedResult) shouldBe ACCEPTED

      afterEach()
    }
    "return NotFound if the user to delete is not found" in {
      val deleteRequest: FakeRequest[AnyContent] = buildDelete("/api/nonexistent_id")
      val deletedResult: Future[Result] = TestApplicationController.delete("nonexistent_id")(deleteRequest)

      status(deletedResult) shouldBe Status.NOT_FOUND
    }
  }


  override def beforeEach(): Unit = await(repository.deleteAll())

  override def afterEach(): Unit = await(repository.deleteAll())
}
