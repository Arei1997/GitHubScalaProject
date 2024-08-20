package controllers

import baseSpec.BaseSpecWithApplication
import model.{User, Repository, FileContent, APIError}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import repository._
import service.{GitHubService, RepositoryService}

import scala.concurrent.{ExecutionContext, Future}

class ApplicationControllerSpec extends BaseSpecWithApplication {

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

  private val testRepo = Repository(name = "test-repo", description = "A test repository", owner = "johndoe")

  private val testFileContent = FileContent(
    content = Some("SGVsbG8sIFdvcmxkIQ=="),
    sha = "abcdef123456",
    path = "hello.txt"
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

    "find a user in the database by login" in {
      beforeEach()

      val request: FakeRequest[JsValue] = buildPost("/api").withBody(Json.toJson(testUser))
      val createdResult: Future[Result] = TestApplicationController.create()(request)

      status(createdResult) shouldBe Status.CREATED

      val readResult: Future[Result] = TestApplicationController.read(testUser.login)(FakeRequest())

      status(readResult) shouldBe OK
      contentAsJson(readResult) shouldBe Json.toJson(testUser)

      afterEach()
    }

    "return NotFound if the user is not found" in {
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

      val updatedUser: User = testUser.copy(followers = 200)
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

  "ApplicationController .getGitHubUser" should {
    "return OK and render the gitHubUser view if the user exists" in {
      val testUsername = testUser.login

      val request = FakeRequest(GET, s"/api/github/users/$testUsername")
      val result = TestApplicationController.getGitHubUser(testUsername).apply(request)

      status(result) mustBe OK
      contentAsString(result) must include(testUser.login)
    }

    "redirect to the index page with an error message if the user is not found" in {
      val testUsername = "unknownuser"
      val request = FakeRequest(GET, s"/api/github/users/$testUsername")

      val result = TestApplicationController.getGitHubUser(testUsername).apply(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.ApplicationController.index().url)
      flash(result).get("error") mustBe Some("User not found")
    }
  }

  "ApplicationController .getGitHubRepo" should {
    "return OK and render the gitHubRepo view if the repo exists" in {
      val testUsername = testUser.login

      when(repoService.getGithubRepo(testUsername)).thenReturn(Future.successful(Right(List(testRepo))))

      val request = FakeRequest(GET, s"/api/github/repos/$testUsername")
      val result = TestApplicationController.getGitHubRepo(testUsername).apply(request)

      status(result) mustBe OK
      contentAsString(result) must include(testRepo.name)
    }

    "return NotFound and an error message if the repo is not found" in {
      val testUsername = "unknownuser"

      when(repoService.getGithubRepo(testUsername)).thenReturn(Future.successful(Left(APIError.BadAPIResponse(Status.NOT_FOUND, "Repository not found"))))

      val request = FakeRequest(GET, s"/api/github/repos/$testUsername")
      val result = TestApplicationController.getGitHubRepo(testUsername).apply(request)

      status(result) mustBe Status.NOT_FOUND
      contentAsJson(result) mustBe Json.obj("error" -> "Repository not found")
    }
  }

  "ApplicationController .getGitHubRepoContents" should {
    "return OK and render the gitHubRepoContents view if the repo contents exist" in {
      val testUsername = testUser.login
      val testRepoName = testRepo.name

      when(repoService.getRepoContents(testUsername, testRepoName)).thenReturn(Future.successful(Right(List(testFileContent))))

      val request = FakeRequest(GET, s"/api/github/repos/$testUsername/$testRepoName/contents")
      val result = TestApplicationController.getGitHubRepoContents(testUsername, testRepoName).apply(request)

      status(result) mustBe OK
      contentAsString(result) must include(testFileContent.path)
    }

    "return NotFound and an error message if the repo contents are not found" in {
      val testUsername = "unknownuser"
      val testRepoName = "unknownrepo"

      when(repoService.getRepoContents(testUsername, testRepoName)).thenReturn(Future.successful(Left(APIError.BadAPIResponse(Status.NOT_FOUND, "Contents not found"))))

      val request = FakeRequest(GET, s"/api/github/repos/$testUsername/$testRepoName/contents")
      val result = TestApplicationController.getGitHubRepoContents(testUsername, testRepoName).apply(request)

      status(result) mustBe Status.NOT_FOUND
      contentAsJson(result) mustBe Json.obj("error" -> "Contents not found")
    }
  }

  "ApplicationController .getGitHubFile" should {
    "return OK and render the gitHubFileContents view if the file exists" in {
      val testUsername = testUser.login
      val testRepoName = testRepo.name
      val testPath = "hello.txt"

      when(repoService.getFileContent(testUsername, testRepoName, testPath)).thenReturn(Future.successful(Right(testFileContent)))

      val request = FakeRequest(GET, s"/api/github/repos/$testUsername/$testRepoName/contents/$testPath")
      val result = TestApplicationController.getGitHubFile(testUsername, testRepoName, testPath).apply(request)

      status(result) mustBe OK
      contentAsString(result) must include(testFileContent.path)
    }

    "return NotFound and an error message if the file is not found" in {
      val testUsername = "unknownuser"
      val testRepoName = "unknownrepo"
      val testPath = "unknownfile.txt"

      when(repoService.getFileContent(testUsername, testRepoName, testPath)).thenReturn(Future.successful(Left(APIError.BadAPIResponse(Status.NOT_FOUND, "File not found"))))

      val request = FakeRequest(GET, s"/api/github/repos/$testUsername/$testRepoName/contents/$testPath")
      val result = TestApplicationController.getGitHubFile(testUsername, testRepoName, testPath).apply(request)

      status(result) mustBe Status.NOT_FOUND
      contentAsString(result) must include("No file content found at")
    }

    "return BadRequest if the file content is not valid base64" in {
      val testUsername = testUser.login
      val testRepoName = testRepo.name
      val testPath = "invalidfile.txt"

      when(repoService.getFileContent(testUsername, testRepoName, testPath)).thenReturn(Future.successful(Right(testFileContent.copy(content = Some("invalid-base64")))))

      val request = FakeRequest(GET, s"/api/github/repos/$testUsername/$testRepoName/contents/$testPath")
      val result = TestApplicationController.getGitHubFile(testUsername, testRepoName, testPath).apply(request)

      status(result) mustBe Status.BAD_REQUEST
      contentAsString(result) must include("Invalid base64 content")
    }
  }

  "ApplicationController .getGitHubFolder" should {
    "return OK and render the gitHubRepoContents view if the folder exists" in {
      val testUsername = testUser.login
      val testRepoName = testRepo.name
      val testPath = "src"

      when(repoService.getRepoFiles(testUsername, testRepoName, testPath)).thenReturn(Future.successful(Right(List(testFileContent))))

      val request = FakeRequest(GET, s"/api/github/repos/$testUsername/$testRepoName/contents/$testPath")
      val result = TestApplicationController.getGitHubFolder(testUsername, testRepoName, testPath).apply(request)

      status(result) mustBe OK
      contentAsString(result) must include(testFileContent.path)
    }

    "return NotFound and an error message if the folder is not found" in {
      val testUsername = "unknownuser"
      val testRepoName = "unknownrepo"
      val testPath = "unknownfolder"

      when(repoService.getRepoFiles(testUsername, testRepoName, testPath)).thenReturn(Future.successful(Left(APIError.BadAPIResponse(Status.NOT_FOUND, "Folder not found"))))

      val request = FakeRequest(GET, s"/api/github/repos/$testUsername/$testRepoName/contents/$testPath")
      val result = TestApplicationController.getGitHubFolder(testUsername, testRepoName, testPath).apply(request)

      status(result) mustBe Status.NOT_FOUND
      contentAsJson(result) mustBe Json.obj("error" -> "Folder not found")
    }
  }

  "ApplicationController .searchGitHubUser" should {
    "redirect to the getGitHubRepo action if a username is provided" in {
      val testUsername = testUser.login
      val request = FakeRequest(GET, s"/api/search?username=$testUsername")
      val result = TestApplicationController.searchGitHubUser().apply(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.ApplicationController.getGitHubRepo(testUsername).url)
    }

    "return BadRequest if no username is provided" in {
      val request = FakeRequest(GET, "/api/search")
      val result = TestApplicationController.searchGitHubUser().apply(request)

      status(result) mustBe Status.BAD_REQUEST
      contentAsString(result) mustBe "Username not provided"
    }
  }

  override def beforeEach(): Unit = await(repository.deleteAll())

  override def afterEach(): Unit = await(repository.deleteAll())
}
