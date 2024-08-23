package controllers

import baseSpec.BaseSpecWithApplication
import cats.data.EitherT
import model.{APIError, Contents, User}
import org.mockito.MockitoSugar.{mock, when}
import play.api.test.FakeRequest
import play.api.http.Status
import play.api.test.Helpers._
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContent, Result}
import service.RepositoryService

import scala.concurrent.Future

class ApplicationControllerSpec extends BaseSpecWithApplication {

  val TestApplicationController = new ApplicationController(
    component,
    repository,
    githubService = null,
    repositoryService = null
  )(executionContext)

  private val user: User = User(
    login = "JamesDev",
    name = Some("James Developer"),
    avatar_url = "http://avatar.url/jamesdev",
    location = Some("London"),
    bio = Some("Scala developer"),
    followers = 100,
    following = 50,
    created_at = "2022-01-01T00:00:00Z"
  )

  val mockRepositoryService: RepositoryService = mock[RepositoryService]


  val username = "testUser"
  val repoName = "testRepo"
  val path = "testPath"
  val fileContent = "Hello, world!"
  val encodedContent = java.util.Base64.getEncoder.encodeToString(fileContent.getBytes("UTF-8"))
  val sha = "testSha"

  val validContents = Contents(
    name = "testFile",
    `type` = "file",
    html_url = s"https://github.com/$username/$repoName/$path",
    url = s"https://api.github.com/repos/$username/$repoName/contents/$path",
    path = path,
    sha = Some(sha),
    content = Some(encodedContent)
  )

  "ApplicationController .create" should {

    "create a user in the database" in {
      beforeEach()

      val request: FakeRequest[JsValue] = buildPost("/api").withBody(Json.toJson(user))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      status(createdResult) shouldBe Status.CREATED
      contentAsJson(createdResult) shouldBe Json.toJson(user)

      afterEach()
    }

    "return a BadRequest for invalid JSON" in {
      val invalidRequest: FakeRequest[JsValue] = buildPost("/api").withBody(Json.obj("invalid" -> "data"))
      val result: Future[Result] = TestApplicationController.create()(invalidRequest)

      status(result) shouldBe Status.BAD_REQUEST
      contentAsString(result) should include("errors")
    }
  }

  "ApplicationController .read" should {

    "find a user in the database by login" in {
      beforeEach()

      val request: FakeRequest[JsValue] = buildPost("/api").withBody(Json.toJson(user))
      val createdResult: Future[Result] = TestApplicationController.create()(request)
      status(createdResult) shouldBe Status.CREATED

      val readResult: Future[Result] = TestApplicationController.read(user.login)(FakeRequest())

      status(readResult) shouldBe OK
      contentAsJson(readResult) shouldBe Json.toJson(user)

      afterEach()
    }

    "return NotFound if the user is not found" in {
      val readResult: Future[Result] = TestApplicationController.read("nonexistent_login")(FakeRequest())

      status(readResult) shouldBe Status.NOT_FOUND
      contentAsString(readResult) should include("User not found")
    }
  }

  "ApplicationController .update" should {

    "update a user in the database" in {
      beforeEach()

      val request: FakeRequest[JsValue] = buildPost("/api").withBody(Json.toJson(user))
      val createdResult: Future[Result] = TestApplicationController.create()(request)
      status(createdResult) shouldBe Status.CREATED

      val updatedUser: User = user.copy(name = Some("Updated Name"))
      val updatedRequest: FakeRequest[JsValue] = buildPut(s"/api/${user.login}").withBody(Json.toJson(updatedUser))
      val updatedResult: Future[Result] = TestApplicationController.update(user.login)(updatedRequest)

      status(updatedResult) shouldBe ACCEPTED

      val readResult: Future[Result] = TestApplicationController.read(user.login)(FakeRequest())
      status(readResult) shouldBe OK
      contentAsJson(readResult) shouldBe Json.toJson(updatedUser)

      afterEach()
    }

    "return NotFound if the user to update is not found" in {
      val updatedUser: User = user.copy(name = Some("Updated Name"))
      val updatedRequest: FakeRequest[JsValue] = buildPut(s"/api/nonexistent_login").withBody(Json.toJson(updatedUser))
      val updatedResult: Future[Result] = TestApplicationController.update("nonexistent_login")(updatedRequest)

      status(updatedResult) shouldBe Status.NOT_FOUND
      contentAsString(updatedResult) should include("User not found")
    }

    "return BadRequest for invalid JSON" in {
      val invalidRequest: FakeRequest[JsValue] = buildPut(s"/api/${user.login}").withBody(Json.obj("invalid" -> "data"))
      val result: Future[Result] = TestApplicationController.update(user.login)(invalidRequest)

      status(result) shouldBe Status.BAD_REQUEST
      contentAsString(result) should include("errors")
    }
  }

  "ApplicationController .delete" should {

    "delete a user in the database" in {
      beforeEach()

      val request: FakeRequest[JsValue] = buildPost("/api").withBody(Json.toJson(user))
      val createdResult: Future[Result] = TestApplicationController.create()(request)
      status(createdResult) shouldBe Status.CREATED

      val deleteRequest: FakeRequest[AnyContent] = buildDelete(s"/api/${user.login}")
      val deletedResult: Future[Result] = TestApplicationController.delete(user.login)(deleteRequest)

      status(deletedResult) shouldBe ACCEPTED
      contentAsString(deletedResult) should include("Item successfully deleted")

      afterEach()
    }

    "return NotFound if the user to delete is not found" in {
      val deleteRequest: FakeRequest[AnyContent] = buildDelete("/api/nonexistent_login")
      val deletedResult: Future[Result] = TestApplicationController.delete("nonexistent_login")(deleteRequest)

      status(deletedResult) shouldBe Status.NOT_FOUND
      contentAsString(deletedResult) should include("Item not found")
    }
  }

  "ApplicationController .index" should {

    "return a list of users" in {
      beforeEach()

      val users = Seq(
        user,
        user.copy(login = "JaneDoe", name = Some("Jane Doe")),
        user.copy(login = "JohnSmith", name = Some("John Smith"))
      )

      users.foreach { u =>
        val request: FakeRequest[JsValue] = buildPost("/api").withBody(Json.toJson(u))
        val result: Future[Result] = TestApplicationController.create()(request)
        status(result) shouldBe Status.CREATED
      }

      val indexResult: Future[Result] = TestApplicationController.index()(FakeRequest())

      status(indexResult) shouldBe OK
      val json = contentAsJson(indexResult).as[Seq[User]]
      json.length shouldBe users.length

      afterEach()
    }

    "return NotFound if the user is not found" in {
      val readResult: Future[Result] = TestApplicationController.read("nonexistent_login")(FakeRequest())

      status(readResult) shouldBe Status.NOT_FOUND // Expecting 404 here
      contentAsString(readResult) should include("User not found") // Check for proper error message
    }
  }

  "getGitHubFile" should {
    "return 200 OK with the decoded file content when the file exists" in {

      when(mockRepositoryService.getFileContent(username, repoName, path))
        .thenReturn(EitherT.rightT[Future, APIError](validContents))

      val request = FakeRequest(GET, s"/api/github/$username/$repoName/contents/$path")
      val result: Future[Result] = TestApplicationController.getGitHubFile(username, repoName, path)(request)

      status(result) shouldBe OK
      contentAsString(result) should include(fileContent)
      contentAsString(result) should include(sha)
    }

    "return 404 Not Found when the file exists but no content is available" in {
      val emptyContent = validContents.copy(content = None)


      when(mockRepositoryService.getFileContent(username, repoName, path))
        .thenReturn(EitherT.rightT[Future, APIError](validContents))

      val request = FakeRequest(GET, s"/api/github/$username/$repoName/contents/$path")
      val result: Future[Result] = TestApplicationController.getGitHubFile(username, repoName, path)(request)

      status(result) shouldBe NOT_FOUND
      contentAsString(result) should include(s"No file content found at $path")
    }

    "return 400 Bad Request when the file content is invalid Base64" in {
      val invalidBase64Content = validContents.copy(content = Some("InvalidBase64Content"))

      when(mockRepositoryService.getFileContent(username, repoName, path))
        .thenReturn(EitherT.rightT[Future, APIError](invalidBase64Content))

      val request = FakeRequest(GET, s"/api/github/$username/$repoName/contents/$path")
      val result: Future[Result] = TestApplicationController.getGitHubFile(username, repoName, path)(request)

      status(result) shouldBe BAD_REQUEST
      contentAsString(result) should include("Invalid base64 content")
    }

    "return the appropriate status code and error message when an API error occurs" in {
      val apiError = APIError.BadAPIResponse(500, "Internal Server Error")

      when(mockRepositoryService.getFileContent(username, repoName, path))
        .thenReturn(EitherT.rightT[Future, APIError](validContents))

      val request = FakeRequest(GET, s"/api/github/$username/$repoName/contents/$path")
      val result: Future[Result] = TestApplicationController.getGitHubFile(username, repoName, path)(request)

      status(result) shouldBe INTERNAL_SERVER_ERROR
      contentAsString(result) should include("Error fetching file content: Internal Server Error")
    }
  }









  override def beforeEach(): Unit = await(repository.deleteAll())

  override def afterEach(): Unit = await(repository.deleteAll())
}
