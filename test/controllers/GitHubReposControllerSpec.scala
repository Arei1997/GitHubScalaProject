package controllers

import model.{APIError, Contents, Delete, FileFormData}
import org.scalatest.matchers.must.Matchers._
import org.scalatestplus.play._
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import service.RepositoryService
import views.html.{fileForm, deleteFileForm}

import scala.concurrent.{ExecutionContext, Future}
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._

class GitHubReposControllerSpec extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar {

  implicit val ec: ExecutionContext = ExecutionContext.global

  val mockRepositoryService: RepositoryService = mock[RepositoryService]
  val controller = new GitHubReposController(mockRepositoryService, stubControllerComponents())

  val testUsername = "johndoe"
  val testRepoName = "test-repo"
  val testPath = "hello.txt"

  val testFileContent = Contents(
    content = Some("SGVsbG8sIFdvcmxkIQ=="),
    sha = "abcdef123456",
    path = testPath
  )

  "GitHubReposController" should {

    "read a file" should {
      "return OK with the file content if the file exists" in {
        when(mockRepositoryService.getFileContent(testUsername, testRepoName, testPath))
          .thenReturn(Future.successful(Right(testFileContent)))

        val request = FakeRequest(GET, s"/github/$testUsername/$testRepoName/contents/$testPath")
        val result: Future[Result] = controller.readFile(testUsername, testRepoName, testPath).apply(request)

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(testFileContent)
      }

      "return NotFound if the file does not exist" in {
        when(mockRepositoryService.getFileContent(testUsername, testRepoName, testPath))
          .thenReturn(Future.successful(Left(APIError.BadAPIResponse(NOT_FOUND, "File not found"))))

        val request = FakeRequest(GET, s"/github/$testUsername/$testRepoName/contents/$testPath")
        val result: Future[Result] = controller.readFile(testUsername, testRepoName, testPath).apply(request)

        status(result) mustBe NOT_FOUND
        contentAsJson(result) mustBe Json.obj("error" -> "File not found")
      }
    }

    "create or update a file" should {
      "return OK with the file details if the file is created or updated successfully" in {
        val message = "Initial commit"
        val content = "Hello, World!"
        val sha = "abcdef123456"
        val requestBody = Json.obj("message" -> message, "content" -> content, "sha" -> sha)

        when(mockRepositoryService.createOrUpdateFile(testUsername, testRepoName, testPath, message, content, Some(sha)))
          .thenReturn(Future.successful(Right(testFileContent)))

        val request = FakeRequest(POST, s"/github/$testUsername/$testRepoName/contents/$testPath").withJsonBody(requestBody)
        val result: Future[Result] = controller.createOrUpdateFile(testUsername, testRepoName, testPath).apply(request)

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(testFileContent)
      }

      "return BadRequest if there is an error while creating or updating the file" in {
        val message = "Initial commit"
        val content = "Hello, World!"
        val sha = "abcdef123456"
        val requestBody = Json.obj("message" -> message, "content" -> content, "sha" -> sha)

        when(mockRepositoryService.createOrUpdateFile(testUsername, testRepoName, testPath, message, content, Some(sha)))
          .thenReturn(Future.successful(Left(APIError.BadAPIResponse(BAD_REQUEST, "Invalid request"))))

        val request = FakeRequest(POST, s"/github/$testUsername/$testRepoName/contents/$testPath").withJsonBody(requestBody)
        val result: Future[Result] = controller.createOrUpdateFile(testUsername, testRepoName, testPath).apply(request)

        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe Json.obj("error" -> "Invalid request")
      }
    }

    "delete a file" should {
      "return OK if the file is deleted successfully" in {
        val message = "Deleting file"
        val sha = "abcdef123456"
        val requestBody = Json.obj("message" -> message, "sha" -> sha)

        when(mockRepositoryService.deleteFile(testUsername, testRepoName, testPath, message, sha))
          .thenReturn(Future.successful(Right("File deleted")))

        val request = FakeRequest(DELETE, s"/github/$testUsername/$testRepoName/contents/$testPath").withJsonBody(requestBody)
        val result: Future[Result] = controller.deleteFile(testUsername, testRepoName, testPath).apply(request)

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.obj("message" -> "File deleted")
      }

      "return BadRequest if there is an error while deleting the file" in {
        val message = "Deleting file"
        val sha = "abcdef123456"
        val requestBody = Json.obj("message" -> message, "sha" -> sha)

        when(mockRepositoryService.deleteFile(testUsername, testRepoName, testPath, message, sha))
          .thenReturn(Future.successful(Left(APIError.BadAPIResponse(BAD_REQUEST, "Invalid request"))))

        val request = FakeRequest(DELETE, s"/github/$testUsername/$testRepoName/contents/$testPath").withJsonBody(requestBody)
        val result: Future[Result] = controller.deleteFile(testUsername, testRepoName, testPath).apply(request)

        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe Json.obj("error" -> "Invalid request")
      }
    }

    "display the create or update file form" should {
      "return OK with the file form view" in {
        val request = FakeRequest(GET, s"/github/$testUsername/$testRepoName/contents/$testPath/createOrUpdate")
        val result: Future[Result] = controller.createFileForm(testUsername, testRepoName, testPath).apply(request)

        status(result) mustBe OK
        contentAsString(result) must include("Create or update file")
      }
    }

    "submit the create or update file form" should {
      "redirect to the repo contents page with success message if the file is created or updated" in {
        val formData = FileFormData("file.txt", "Initial commit", "Hello, World!")
        when(mockRepositoryService.getFileContent(testUsername, testRepoName, formData.fileName))
          .thenReturn(Future.successful(Left(APIError.BadAPIResponse(NOT_FOUND, "File not found"))))

        when(mockRepositoryService.createOrUpdateFile(testUsername, testRepoName, formData.fileName, formData.message, formData.content, None))
          .thenReturn(Future.successful(Right(testFileContent)))

        val request = FakeRequest(POST, s"/github/$testUsername/$testRepoName/contents/$testPath")
          .withFormUrlEncodedBody("fileName" -> formData.fileName, "message" -> formData.message, "content" -> formData.content)

        val result: Future[Result] = controller.submitFileForm(testUsername, testRepoName, testPath).apply(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.ApplicationController.getGitHubRepoContents(testUsername, testRepoName).url)
        flash(result).get("success") mustBe Some("File created successfully")
      }

      "return BadRequest with form errors if the form submission fails" in {
        val formData = FileFormData("file.txt", "Initial commit", "Hello, World!")
        when(mockRepositoryService.getFileContent(testUsername, testRepoName, formData.fileName))
          .thenReturn(Future.successful(Left(APIError.BadAPIResponse(NOT_FOUND, "File not found"))))

        when(mockRepositoryService.createOrUpdateFile(testUsername, testRepoName, formData.fileName, formData.message, formData.content, None))
          .thenReturn(Future.successful(Left(APIError.BadAPIResponse(BAD_REQUEST, "Invalid request"))))

        val request = FakeRequest(POST, s"/github/$testUsername/$testRepoName/contents/$testPath")
          .withFormUrlEncodedBody("fileName" -> formData.fileName, "message" -> formData.message, "content" -> formData.content)

        val result: Future[Result] = controller.submitFileForm(testUsername, testRepoName, testPath).apply(request)

        status(result) mustBe BAD_REQUEST
        contentAsString(result) must include("Create or update file")
      }
    }

    "display the delete file form" should {
      "return OK with the delete file form view" in {
        val request = FakeRequest(GET, s"/github/$testUsername/$testRepoName/contents/$testPath/delete")
        val result: Future[Result] = controller.deleteFileForm(testUsername, testRepoName, testPath).apply(request)

        status(result) mustBe OK
        contentAsString(result) must include("Delete file")
      }
    }

    "submit the delete file form" should {
      "redirect to the delete file form with success message if the file is deleted" in {
        val deleteData = Delete("Deleting file", "abcdef123456")

        when(mockRepositoryService.deleteFile(testUsername, testRepoName, testPath, deleteData.message, deleteData.sha))
          .thenReturn(Future.successful(Right("File deleted")))

        val request = FakeRequest(POST, s"/github/$testUsername/$testRepoName/contents/$testPath/delete")
          .withFormUrlEncodedBody("message" -> deleteData.message, "sha" -> deleteData.sha)

        val result: Future[Result] = controller.submitDeleteFileForm(testUsername, testRepoName, testPath).apply(request)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.GitHubReposController.deleteFileForm(testUsername, testRepoName, testPath).url)
        flash(result).get("success") mustBe Some("File deleted successfully")
      }

      "return BadRequest with form errors if the form submission fails" in {
        val deleteData = Delete("Deleting file", "abcdef123456")

        when(mockRepositoryService.deleteFile(testUsername, testRepoName, testPath, deleteData.message, deleteData.sha))
          .thenReturn(Future.successful(Left(APIError.BadAPIResponse(BAD_REQUEST, "Invalid request"))))

        val request = FakeRequest(POST, s"/github/$testUsername/$testRepoName/contents/$testPath/delete")
          .withFormUrlEncodedBody("message" -> deleteData.message, "sha" -> deleteData.sha)

        val result: Future[Result] = controller.submitDeleteFileForm(testUsername, testRepoName, testPath).apply(request)

        status(result) mustBe BAD_REQUEST
        contentAsString(result) must include("Delete file")
      }
    }
  }
}
