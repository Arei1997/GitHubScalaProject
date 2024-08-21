package controllers

import cats.data.EitherT
import model.{APIError, Contents, Repository, User}
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import org.mockito.scalatest.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test._
import repository.DataRepository
import service.{GitHubService, RepositoryService}
import org.mongodb.scala.result.DeleteResult

import scala.concurrent.{ExecutionContext, Future}

class ApplicationControllerSpec extends PlaySpec with Results with MockitoSugar with ScalaFutures {

  // Add an implicit ExecutionContext
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  // Mock dependencies
  val mockDataRepository = mock[DataRepository]
  val mockGitHubService = mock[GitHubService]
  val mockRepositoryService = mock[RepositoryService]
  val controllerComponents = stubControllerComponents()

  // Controller instance
  val controller = new ApplicationController(
    controllerComponents,
    mockDataRepository,
    mockGitHubService,
    mockRepositoryService
  )

  "index" should {
    "return a list of users when users exist" in {
      val users = Seq(User("testuser", Some("Test User"), "avatar_url", Some("location"), Some("bio"), 1, 1, "created_at"))
      when(mockDataRepository.index()).thenReturn(Future.successful(Right(users)))

      val result = controller.index().apply(FakeRequest(GET, "/"))

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(users)
    }

    "return 404 when no users are found" in {
      when(mockDataRepository.index()).thenReturn(Future.successful(Left(APIError.BadAPIResponse(404, "Users cannot be found"))))

      val result = controller.index().apply(FakeRequest(GET, "/"))

      status(result) mustBe NOT_FOUND
      contentAsJson(result) mustBe Json.toJson("Users cannot be found")
    }
  }

  "create" should {
    "create a user successfully" in {
      val userJson: JsValue = Json.parse(
        """{
          | "login": "testuser",
          | "name": "Test User",
          | "avatar_url": "avatar_url",
          | "location": "location",
          | "bio": "bio",
          | "followers": 1,
          | "following": 1,
          | "created_at": "created_at"
          |}""".stripMargin)
      val user = userJson.as[User]

      when(mockDataRepository.create(any[User])).thenReturn(Future.successful(Right(user)))

      val request = FakeRequest(POST, "/").withBody(userJson).withHeaders(CONTENT_TYPE -> "application/json")
      val result = controller.create().apply(request)

      status(result) mustBe CREATED
      contentAsJson(result) mustBe Json.toJson(user)
    }

    "return 400 for invalid JSON" in {
      val invalidJson: JsValue = Json.parse("""{"login": "testuser"}""")

      val request = FakeRequest(POST, "/").withBody(invalidJson).withHeaders(CONTENT_TYPE -> "application/json")
      val result = controller.create().apply(request)

      status(result) mustBe BAD_REQUEST
    }
  }

  "getGitHubRepo" should {
    "return a list of repositories if found" in {
      val username = "testuser"
      val repositories = List(
        Repository("repo1", "https://github.com/testuser/repo1", Some("Test repository 1")),
        Repository("repo2", "https://github.com/testuser/repo2", Some("Test repository 2"))
      )

      // Mock the getGithubRepo to return a successful Right with the list of repositories
      when(mockRepositoryService.getGithubRepo(username))
        .thenReturn(EitherT(Future.successful(Right(repositories): Either[APIError, List[Repository]])))

      val result = controller.getGitHubRepo(username).apply(FakeRequest(GET, "/"))

      status(result) mustBe OK
      contentAsString(result) must include("repo1")
      contentAsString(result) must include("repo2")
    }

    "return a 404 status if repositories are not found" in {
      val username = "testuser"

      // Mock the getGithubRepo to return a Left indicating repositories not found
      when(mockRepositoryService.getGithubRepo(username))
        .thenReturn(EitherT(Future.successful(Left(APIError.BadAPIResponse(404, "Repositories not found")): Either[APIError, List[Repository]])))

      val result = controller.getGitHubRepo(username).apply(FakeRequest(GET, "/"))

      status(result) mustBe NOT_FOUND
      contentAsJson(result) mustBe Json.obj("error" -> "Repositories not found")
    }

    "return a 500 status if there is a server error" in {
      val username = "testuser"

      // Mock the getGithubRepo to return a Left indicating a server error
      when(mockRepositoryService.getGithubRepo(username))
        .thenReturn(EitherT(Future.successful(Left(APIError.BadAPIResponse(500, "Internal server error")): Either[APIError, List[Repository]])))

      val result = controller.getGitHubRepo(username).apply(FakeRequest(GET, "/"))

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe Json.obj("error" -> "Internal server error")
    }
  }


  "delete" should {
    "return 202 Accepted when a user is successfully deleted" in {
      val login = "testuser"

      // Mock the DeleteResult with a successful deletion count
      val deleteResult = mock[DeleteResult]
      when(deleteResult.getDeletedCount).thenReturn(1L)
      when(mockDataRepository.delete(login)).thenReturn(Future.successful(Right(deleteResult)))

      val result = controller.delete(login).apply(FakeRequest(DELETE, s"/users/$login"))

      status(result) mustBe ACCEPTED
      contentAsJson(result) mustBe Json.toJson("Item successfully deleted")
    }

    "return 404 NotFound when the user to be deleted is not found" in {
      val login = "testuser"

      // Mock the DeleteResult with 0 deletion count indicating not found
      val deleteResult = mock[DeleteResult]
      when(deleteResult.getDeletedCount).thenReturn(0L)
      when(mockDataRepository.delete(login)).thenReturn(Future.successful(Right(deleteResult)))

      val result = controller.delete(login).apply(FakeRequest(DELETE, s"/users/$login"))

      status(result) mustBe NOT_FOUND
      contentAsJson(result) mustBe Json.toJson("Item not found")
    }

    "return 500 InternalServerError when there is a server error during deletion" in {
      val login = "testuser"

      // Mock the delete method to return an APIError indicating server error
      when(mockDataRepository.delete(login))
        .thenReturn(Future.successful(Left(APIError.BadAPIResponse(500, "Internal server error"))))

      val result = controller.delete(login).apply(FakeRequest(DELETE, s"/users/$login"))

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe Json.obj("error" -> "Internal server error")
    }
  }


  "read" should {
    "return the user when found" in {
      val login = "testuser"
      val user = User(login, Some("Test User"), "avatar_url", Some("location"), Some("bio"), 1, 1, "created_at")

      // Mock the dataRepository.read to return a successful Right with the user
      when(mockDataRepository.read(login)).thenReturn(Future.successful(Right(user)))

      val result = controller.read(login).apply(FakeRequest(GET, s"/users/$login"))

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(user)
    }

    "return 404 NotFound when the user is not found" in {
      val login = "testuser"

      // Mock the dataRepository.read to return a Left indicating user not found
      when(mockDataRepository.read(login)).thenReturn(Future.successful(Left(APIError.BadAPIResponse(404, "User not found"))))

      val result = controller.read(login).apply(FakeRequest(GET, s"/users/$login"))

      status(result) mustBe NOT_FOUND
      contentAsJson(result) mustBe Json.toJson("User not found")
    }

    "return 500 InternalServerError when there is a server error" in {
      val login = "testuser"

      // Mock the dataRepository.read to return a Left indicating a server error
      when(mockDataRepository.read(login)).thenReturn(Future.successful(Left(APIError.BadAPIResponse(500, "Internal server error"))))

      val result = controller.read(login).apply(FakeRequest(GET, s"/users/$login"))

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe Json.toJson("Bad response from upstream; got status: 500, and got reason Internal server error")

    }
  }

  "update" should {
    "return 202 Accepted when the user is successfully updated" in {
      val login = "testuser"
      val userJson: JsValue = Json.parse(
        """{
          | "login": "testuser",
          | "name": "Updated User",
          | "avatar_url": "avatar_url",
          | "location": "updated location",
          | "bio": "updated bio",
          | "followers": 2,
          | "following": 2,
          | "created_at": "created_at"
          |}""".stripMargin)
      val user = userJson.as[User]

      // Mock the dataRepository.update to return a successful update result
      when(mockDataRepository.update(login, user)).thenReturn(Future.successful(Right(1L)))

      val request = FakeRequest(PUT, s"/users/$login").withBody(userJson).withHeaders(CONTENT_TYPE -> "application/json")
      val result = controller.update(login).apply(request)

      status(result) mustBe ACCEPTED
    }

    "return 404 NotFound when the user to be updated is not found" in {
      val login = "testuser"
      val userJson: JsValue = Json.parse(
        """{
          | "login": "testuser",
          | "name": "Updated User",
          | "avatar_url": "avatar_url",
          | "location": "updated location",
          | "bio": "updated bio",
          | "followers": 2,
          | "following": 2,
          | "created_at": "created_at"
          |}""".stripMargin)
      val user = userJson.as[User]

      // Mock the dataRepository.update to return a 0 update result, indicating not found
      when(mockDataRepository.update(login, user)).thenReturn(Future.successful(Right(0L)))

      val request = FakeRequest(PUT, s"/users/$login").withBody(userJson).withHeaders(CONTENT_TYPE -> "application/json")
      val result = controller.update(login).apply(request)

      status(result) mustBe NOT_FOUND
      contentAsJson(result) mustBe Json.obj("error" -> "User not found")
    }

    "return 500 InternalServerError when there is a server error during update" in {
      val login = "testuser"
      val userJson: JsValue = Json.parse(
        """{
          | "login": "testuser",
          | "name": "Updated User",
          | "avatar_url": "avatar_url",
          | "location": "updated location",
          | "bio": "updated bio",
          | "followers": 2,
          | "following": 2,
          | "created_at": "created_at"
          |}""".stripMargin)
      val user = userJson.as[User]

      // Mock the dataRepository.update to return an APIError indicating a server error
      when(mockDataRepository.update(login, user)).thenReturn(Future.successful(Left(APIError.BadAPIResponse(500, "Internal server error"))))

      val request = FakeRequest(PUT, s"/users/$login").withBody(userJson).withHeaders(CONTENT_TYPE -> "application/json")
      val result = controller.update(login).apply(request)

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe Json.toJson("Internal server error")
    }

    "return 400 BadRequest when the input JSON is invalid" in {
      val login = "testuser"
      val invalidJson: JsValue = Json.parse("""{"login": "testuser"}""")  // Missing required fields

      val request = FakeRequest(PUT, s"/users/$login").withBody(invalidJson).withHeaders(CONTENT_TYPE -> "application/json")
      val result = controller.update(login).apply(request)

      status(result) mustBe BAD_REQUEST
    }
  }

  "getGitHubFile" should {

    "return the decoded file content when valid base64 content is provided" in {
      val username = "testuser"
      val repoName = "testrepo"
      val path = "testfile.txt"
      val base64Content = java.util.Base64.getEncoder.encodeToString("Hello, World!".getBytes("UTF-8"))
      val contents = Contents("testfile.txt", "file", "html_url", "url", path, "sha", Some(base64Content))

      when(mockRepositoryService.getFileContent(username, repoName, path))
        .thenReturn(EitherT(Future.successful(Right(contents): Either[APIError, Contents])))

      val result = controller.getGitHubFile(username, repoName, path).apply(FakeRequest(GET, "/"))

      status(result) mustBe OK
      contentAsString(result) must include("Hello, World!")
    }

    "return 404 when the file content is missing" in {
      val username = "testuser"
      val repoName = "testrepo"
      val path = "testfile.txt"
      val contents = Contents("testfile.txt", "file", "html_url", "url", path, "sha", None)

      when(mockRepositoryService.getFileContent(username, repoName, path))
        .thenReturn(EitherT(Future.successful(Right(contents): Either[APIError, Contents])))

      val result = controller.getGitHubFile(username, repoName, path).apply(FakeRequest(GET, "/"))

      status(result) mustBe NOT_FOUND
      contentAsString(result) must include("No file content found at")
    }

    "return 400 when the base64 content is invalid" in {
      val username = "testuser"
      val repoName = "testrepo"
      val path = "testfile.txt"
      val invalidBase64Content = "invalid_base64_content"
      val contents = Contents("testfile.txt", "file", "html_url", "url", path, "sha", Some(invalidBase64Content))

      when(mockRepositoryService.getFileContent(username, repoName, path))
        .thenReturn(EitherT(Future.successful(Right(contents): Either[APIError, Contents])))

      val result = controller.getGitHubFile(username, repoName, path).apply(FakeRequest(GET, "/"))

      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include("Invalid base64 content")
    }

    "return 500 when there is an error fetching the file content" in {
      val username = "testuser"
      val repoName = "testrepo"
      val path = "testfile.txt"

      when(mockRepositoryService.getFileContent(username, repoName, path))
        .thenReturn(EitherT(Future.successful(Left(APIError.BadAPIResponse(500, "Internal server error")): Either[APIError, Contents])))

      val result = controller.getGitHubFile(username, repoName, path).apply(FakeRequest(GET, "/"))

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsString(result) must include("Error fetching file content: Internal server error")
    }
  }

  "searchGitHubUser" should {

    "redirect to getGitHubRepo when username is provided" in {
      val username = "testuser"


      val request = FakeRequest(GET, s"/searchGitHubUser?username=$username")

      val result = controller.searchGitHubUser().apply(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.ApplicationController.getGitHubRepo(username).url)
    }

    "return BadRequest when username is not provided" in {


      val request = FakeRequest(GET, "/searchGitHubUser")

      val result = controller.searchGitHubUser().apply(request)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include("Username not provided")
    }
  }





}