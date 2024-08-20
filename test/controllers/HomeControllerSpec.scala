package controllers

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import org.scalatestplus.play.{BaseOneAppPerTest, PlaySpec}
import org.mockito.MockitoSugar
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test._
import service.GitHubService
import cats.data.EitherT
import model.{APIError, Repository}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class HomeControllerSpec extends PlaySpec with GuiceOneAppPerTest with MockitoSugar {

  "HomeController" should {

    "render the index page with the top Scala repositories" in {
      val mockGitHubService = mock[GitHubService]
      val fakeControllerComponents = stubControllerComponents()

      val repositories = Seq(
        Repository("repo1", "http://example.com/repo1", Some("Description 1")),
        Repository("repo2", "http://example.com/repo2", Some("Description 2"))
      )

      when(mockGitHubService.getTopScalaRepositories()).thenReturn(
        EitherT.rightT[Future, APIError](repositories)
      )

      val controller = new HomeController(fakeControllerComponents, mockGitHubService)
      val result: Future[Result] = controller.index().apply(FakeRequest(GET, "/"))

      status(result) mustBe OK
      contentType(result) mustBe Some("text/html")
      contentAsString(result) must include("Top Scala Repositories on GitHub")
      contentAsString(result) must include("repo1")
      contentAsString(result) must include("repo2")
    }

    "render an error page when the GitHub service fails" in {
      val mockGitHubService = mock[GitHubService]
      val fakeControllerComponents = stubControllerComponents()

      when(mockGitHubService.getTopScalaRepositories()).thenReturn(
        EitherT.leftT[Future, Seq[Repository]](APIError.BadAPIResponse(500, "Internal Server Error"))
      )

      val controller = new HomeController(fakeControllerComponents, mockGitHubService)
      val result: Future[Result] = controller.index().apply(FakeRequest(GET, "/"))

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentType(result) mustBe Some("text/html")
      contentAsString(result) must include("Oops! Something went wrong.")
      contentAsString(result) must include("Internal Server Error")
    }
  }
}
