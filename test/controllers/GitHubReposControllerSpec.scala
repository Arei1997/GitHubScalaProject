import controllers.{GitHubReposController, routes}
import model.{APIError, Contents, Delete, FileFormData}
import org.mockito.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsEmpty, ControllerComponents, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import service.RepositoryService

import scala.concurrent.{ExecutionContext, Future}

class GitHubReposControllerSpec
  extends AsyncWordSpec
    with Matchers
    with ScalaFutures
    with MockitoSugar {

  // Explicit ExecutionContext
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  // Mocking dependencies
  val mockRepositoryService: RepositoryService = mock[RepositoryService]
  val controllerComponents: ControllerComponents = stubControllerComponents()

  // Instantiate the controller with mocked service
  val controller = new GitHubReposController(mockRepositoryService, controllerComponents)(ec)

  // Sample data
  val username = "testUser"
  val repoName = "testRepo"
  val path = "testPath"
  val fileContent = Contents("file1", "file", "html_url1", "url1", "path1", Some("sha1"), Some("Hello, World!"))
  val errorResponse = APIError.BadAPIResponse(404, "Not Found")
  val deleteFormData = Delete("Commit message", "sha1")

  "GitHubReposController" should {

    "return file content as JSON for readFile" in {
      when(mockRepositoryService.getFileContent(username, repoName, path))
        .thenReturn(Future.successful(Right(fileContent)))

      val request = FakeRequest(GET, s"/github/$username/$repoName/$path")
      val result = controller.readFile(username, repoName, path)(request)

      status(result) shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(fileContent)
    }

    "return error JSON when file content is not found for readFile" in {
      when(mockRepositoryService.getFileContent(username, repoName, path))
        .thenReturn(Future.successful(Left(errorResponse)))

      val request = FakeRequest(GET, s"/github/$username/$repoName/$path")
      val result = controller.readFile(username, repoName, path)(request)

      status(result) shouldBe NOT_FOUND
      (contentAsJson(result) \ "error").as[String] shouldBe "Not Found"
    }

    "create or update file and return file content as JSON" in {
      val jsonBody: JsValue = Json.obj("message" -> "Commit message", "content" -> "New content", "sha" -> "sha1")
      when(mockRepositoryService.createOrUpdateFile(username, repoName, path, "Commit message", "New content", Some("sha1")))
        .thenReturn(Future.successful(Right(fileContent)))

      val request = FakeRequest(POST, s"/github/$username/$repoName/$path").withJsonBody(jsonBody)
      val result = controller.createOrUpdateFile(username, repoName, path)(request)

      status(result) shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(fileContent)
    }

    "delete file and return confirmation as JSON" in {
      val jsonBody: JsValue = Json.obj("message" -> "Commit message", "sha" -> "sha1")
      when(mockRepositoryService.deleteFile(username, repoName, path, "Commit message", "sha1"))
        .thenReturn(Future.successful(Right(Delete("Commit message", "sha1"))))

      val request = FakeRequest(DELETE, s"/github/$username/$repoName/$path").withJsonBody(jsonBody)
      val result = controller.deleteFile(username, repoName, path)(request)

      status(result) shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(Delete("Commit message", "sha1"))
    }

    "return error JSON when deleting a non-existing file" in {
      val jsonBody: JsValue = Json.obj("message" -> "Commit message", "sha" -> "sha1")
      when(mockRepositoryService.deleteFile(username, repoName, path, "Commit message", "sha1"))
        .thenReturn(Future.successful(Left(errorResponse)))

      val request = FakeRequest(DELETE, s"/github/$username/$repoName/$path").withJsonBody(jsonBody)
      val result = controller.deleteFile(username, repoName, path)(request)

      status(result) shouldBe NOT_FOUND
      (contentAsJson(result) \ "error").as[String] shouldBe "Not Found"
    }

    "render create file form" in {
      val request = FakeRequest(GET, s"/github/$username/$repoName/$path/createForm")
      val result = controller.createFileForm(username, repoName, path)(request)

      status(result) shouldBe OK
      contentAsString(result) should include("Create File")
    }

    "submit file form and redirect to repo contents" in {
      val formData = FileFormData("filename", "Commit message", "New content", None)
      when(mockRepositoryService.createOrUpdateFile(username, repoName, "filename", "Commit message", "New content", None))
        .thenReturn(Future.successful(Right(fileContent)))

      val request = FakeRequest(POST, s"/github/$username/$repoName/$path/submitForm")
        .withFormUrlEncodedBody("fileName" -> "filename", "message" -> "Commit message", "content" -> "New content")

      val result = controller.submitCreateFileForm(username, repoName, path)(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.ApplicationController.getGitHubRepoContents(username, repoName).url)
    }

    "return repo languages as JSON" in {
      val languages = Map("Scala" -> 60.0, "Java" -> 40.0)
      when(mockRepositoryService.getRepoLanguagesWithPercentage(username, repoName))
        .thenReturn(Future.successful(Right(languages)))

      val request = FakeRequest(GET, s"/github/$username/$repoName/languages")
      val result = controller.getRepoLanguages(username, repoName)(request)

      status(result) shouldBe OK
      contentAsJson(result) shouldBe Json.toJson(languages)
    }
  }
}
