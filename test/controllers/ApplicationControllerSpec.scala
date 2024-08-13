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
    service
  )(executionContext)

  private val user: User = User(
    "JamesDev",
    "today",
    Some("London"),
    100,
    789767
  )

  "ApplicationController .create" should {

    "create a book in the database" in {
      beforeEach()

      val request: FakeRequest[JsValue] = buildPost("/api").withBody(Json.toJson(user))
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

      val request: FakeRequest[JsValue] = buildGet(s"/api/${user.login}").withBody(Json.toJson(user))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      status(createdResult) shouldBe Status.CREATED

      val readResult: Future[Result] = TestApplicationController.read(user.login)(FakeRequest())

      status(readResult) shouldBe OK
      contentAsJson(readResult) shouldBe Json.toJson(user)

      afterEach()
    }
    "return NotFound if the book is not found" in {
      val readResult: Future[Result] = TestApplicationController.read("nonexistent_id")(FakeRequest())

      status(readResult) shouldBe Status.NOT_FOUND
    }
  }

  "ApplicationController .update" should {
    "update a user in the database" in {
      beforeEach()

      val request: FakeRequest[JsValue] = buildPost("/api").withBody(Json.toJson(user))
      val createdResult: Future[Result] = TestApplicationController.create()(request)
      status(createdResult) shouldBe Status.CREATED

      val updatedUser: User = user.copy(login = "Updated login")
      val updatedRequest: FakeRequest[JsValue] = buildPut(s"/api/${user.login}").withBody(Json.toJson(updatedUser))
      val updatedResult: Future[Result] = TestApplicationController.update(user.login)(updatedRequest)

      status(updatedResult) shouldBe ACCEPTED

      val readResult: Future[Result] = TestApplicationController.read(user.login)(FakeRequest())
      status(readResult) shouldBe OK
      contentAsJson(readResult) shouldBe Json.toJson(updatedUser)

      afterEach()
    }
    "return NotFound if the user to update is not found" in {
      val updatedUser: User = user.copy(login = "Updated Name")
      val updatedRequest: FakeRequest[JsValue] = buildPut(s"/api/nonexistent_id").withBody(Json.toJson(updatedUser))
      val updatedResult: Future[Result] = TestApplicationController.update("nonexistent_id")(updatedRequest)

      status(updatedResult) shouldBe Status.NOT_FOUND
    }

    "return BadRequest for invalid JSON" in {
      val invalidRequest: FakeRequest[JsValue] = buildPut(s"/api/${user.login}").withBody(Json.obj("invalid" -> "data"))
      val result: Future[Result] = TestApplicationController.update(user.login)(invalidRequest)

      status(result) shouldBe Status.BAD_REQUEST
    }
  }


  "ApplicationController .delete" should {
    "delete a book in the database" in {
      beforeEach()
      val request: FakeRequest[JsValue] = buildPost("/api").withBody(Json.toJson(user))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      status(createdResult) shouldBe CREATED

      val deleteRequest: FakeRequest[AnyContent] = buildDelete(s"/api/${user.login}")
      val deletedResult: Future[Result] = TestApplicationController.delete(user.login)(deleteRequest)

      status(deletedResult) shouldBe ACCEPTED

      afterEach()
    }
    "return NotFound if the book to delete is not found" in {
      val deleteRequest: FakeRequest[AnyContent] = buildDelete("/api/nonexistent_id")
      val deletedResult: Future[Result] = TestApplicationController.delete("nonexistent_id")(deleteRequest)

      status(deletedResult) shouldBe Status.NOT_FOUND
    }
  }


  override def beforeEach(): Unit = await(repository.deleteAll())

  override def afterEach(): Unit = await(repository.deleteAll())
}
