//package controllers
//
//import model.{APIError, Contents, Delete, FileFormData}
//import org.mockito.MockitoSugar
//import org.scalatest.concurrent.ScalaFutures
//import org.scalatest.matchers.must.Matchers
//import org.scalatest.wordspec.AnyWordSpec
//import play.api.libs.json.{JsValue, Json}
//import play.api.mvc.{Request, Result}
//import play.api.test.Helpers._
//import play.api.test.{FakeRequest, Helpers}
//import service.RepositoryService
//
//import scala.concurrent.{ExecutionContext, Future}
//
//class GitHubReposControllerSpec extends AnyWordSpec with Matchers with MockitoSugar with ScalaFutures {
//
//  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global
//
//  "GitHubReposController" should {
//
//    "read a file's content successfully" in {
//      val mockRepositoryService = mock[RepositoryService]
//      val controller = new GitHubReposController(mockRepositoryService, Helpers.stubControllerComponents())(ec)
//
//      val fileContent = Contents(
//        name = "example.txt",
//        `type` = "file",
//        html_url = "http://example.com",
//        url = "http://example.com",
//        path = "example.txt",
//        sha = "sampleSHA",
//        content = Some("SGVsbG8gV29ybGQ=") // "Hello World" in Base64
//      )
//
//      when(mockRepositoryService.getFileContent("username", "repoName", "path"))
//        .thenReturn(Future.successful(Right(fileContent)))
//
//      val result: Future[Result] = controller.readFile("username", "repoName", "path").apply(FakeRequest(GET, "/"))
//
//      status(result) mustBe OK
//      contentType(result) mustBe Some("application/json")
//      contentAsJson(result) mustBe Json.toJson(fileContent)
//    }
//
//    "return an error when failing to read a file's content" in {
//      val mockRepositoryService = mock[RepositoryService]
//      val controller = new GitHubReposController(mockRepositoryService, Helpers.stubControllerComponents())(ec)
//
//      when(mockRepositoryService.getFileContent("username", "repoName", "path"))
//        .thenReturn(Future.successful(Left(APIError.BadAPIResponse(404, "Not Found"))))
//
//      val result: Future[Result] = controller.readFile("username", "repoName", "path").apply(FakeRequest(GET, "/"))
//
//      status(result) mustBe NOT_FOUND
//      contentType(result) mustBe Some("application/json")
//      (contentAsJson(result) \ "error").as[String] mustBe "Not Found"
//    }
//
//    "create or update a file successfully" in {
//      val mockRepositoryService = mock[RepositoryService]
//      val controller = new GitHubReposController(mockRepositoryService, Helpers.stubControllerComponents())(ec)
//
//      val fileContent = Contents(
//        name = "example.txt",
//        `type` = "file",
//        html_url = "http://example.com",
//        url = "http://example.com",
//        path = "example.txt",
//        sha = "sampleSHA",
//        content = Some("SGVsbG8gV29ybGQ=") // "Hello World" in Base64
//      )
//
//      when(mockRepositoryService.createOrUpdateFile("username", "repoName", "path", "commit message", "SGVsbG8gV29ybGQ=", None))
//        .thenReturn(Future.successful(Right(fileContent)))
//
//      val requestBody: JsValue = Json.parse(
//        """
//          |{
//          | "message": "commit message",
//          | "content": "SGVsbG8gV29ybGQ=",
//          | "sha": null
//          |}
//          |""".stripMargin)
//
//      val request: Request[JsValue] = FakeRequest(PUT, "/")
//        .withJsonBody(requestBody)
//        .withHeaders("Content-Type" -> "application/json")
//        .asInstanceOf[Request[JsValue]]
//
//      val result: Future[Result] = controller.createOrUpdateFile("username", "repoName", "path").apply(request)
//
//      status(result) mustBe OK
//      contentType(result) mustBe Some("application/json")
//      contentAsJson(result) mustBe Json.toJson(fileContent)
//    }
//
//    "return an error when failing to create or update a file" in {
//      val mockRepositoryService = mock[RepositoryService]
//      val controller = new GitHubReposController(mockRepositoryService, Helpers.stubControllerComponents())(ec)
//
//      when(mockRepositoryService.createOrUpdateFile("username", "repoName", "path", "commit message", "SGVsbG8gV29ybGQ=", None))
//        .thenReturn(Future.successful(Left(APIError.BadAPIResponse(500, "Internal Server Error"))))
//
//      val requestBody: JsValue = Json.parse(
//        """
//          |{
//          | "message": "commit message",
//          | "content": "SGVsbG8gV29ybGQ=",
//          | "sha": null
//          |}
//          |""".stripMargin)
//
//      val request: Request[JsValue] = FakeRequest(PUT, "/")
//        .withJsonBody(requestBody)
//        .withHeaders("Content-Type" -> "application/json")
//        .asInstanceOf[Request[JsValue]]
//
//      val result: Future[Result] = controller.createOrUpdateFile("username", "repoName", "path").apply(request)
//
//      status(result) mustBe INTERNAL_SERVER_ERROR
//      contentType(result) mustBe Some("application/json")
//      (contentAsJson(result) \ "error").as[String] mustBe "Internal Server Error"
//    }
//
//    "delete a file successfully" in {
//      val mockRepositoryService = mock[RepositoryService]
//      val controller = new GitHubReposController(mockRepositoryService, Helpers.stubControllerComponents())(ec)
//
//      val fileContent = Contents(
//        name = "example.txt",
//        `type` = "file",
//        html_url = "http://example.com",
//        url = "http://example.com",
//        path = "example.txt",
//        sha = "sampleSHA",
//        content = Some("SGVsbG8gV29ybGQ=") // "Hello World" in Base64
//      )
//
//      when(mockRepositoryService.deleteFile("username", "repoName", "path", "delete message", "sampleSHA"))
//        .thenReturn(Future.successful(Right(fileContent)))
//
//      val requestBody: JsValue = Json.parse(
//        """
//          |{
//          | "message": "delete message",
//          | "sha": "sampleSHA"
//          |}
//          |""".stripMargin)
//
//      val request: Request[JsValue] = FakeRequest(DELETE, "/")
//        .withJsonBody(requestBody)
//        .withHeaders("Content-Type" -> "application/json")
//        .asInstanceOf[Request[JsValue]]
//
//      val result: Future[Result] = controller.deleteFile("username", "repoName", "path").apply(request)
//
//      status(result) mustBe OK
//      contentType(result) mustBe Some("application/json")
//      contentAsJson(result) mustBe Json.toJson(fileContent)
//    }
//
//    "return an error when failing to delete a file" in {
//      val mockRepositoryService = mock[RepositoryService]
//      val controller = new GitHubReposController(mockRepositoryService, Helpers.stubControllerComponents())(ec)
//
//      when(mockRepositoryService.deleteFile("username", "repoName", "path", "delete message", "sampleSHA"))
//        .thenReturn(Future.successful(Left(APIError.BadAPIResponse(500, "Internal Server Error"))))
//
//      val requestBody: JsValue = Json.parse(
//        """
//          |{
//          | "message": "delete message",
//          | "sha": "sampleSHA"
//          |}
//          |""".stripMargin)
//
//      val request: Request[JsValue] = FakeRequest(DELETE, "/")
//        .withJsonBody(requestBody)
//        .withHeaders("Content-Type" -> "application/json")
//        .asInstanceOf[Request[JsValue]]
//
//      val result: Future[Result] = controller.deleteFile("username", "repoName", "path").apply(request)
//
//      status(result) mustBe INTERNAL_SERVER_ERROR
//      contentType(result) mustBe Some("application/json")
//      (contentAsJson(result) \ "error").as[String] mustBe "Internal Server Error"
//    }
//  }
//}
