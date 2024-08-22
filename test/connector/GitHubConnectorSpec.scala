package connector

import baseSpec.BaseSpecWithApplication
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import com.github.tomakehurst.wiremock.WireMockServer
import model.{User, APIError, CreateOrUpdate, Delete}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.BeforeAndAfterAll
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{Json, OFormat}
import play.api.test.Helpers._

class GitHubConnectorSpec extends BaseSpecWithApplication with ScalaFutures with BeforeAndAfterAll {

  val wireMockServer = new WireMockServer(wireMockConfig().dynamicPort())

  override def beforeAll(): Unit = {
    super.beforeAll()
    wireMockServer.start()
  }

  override def fakeApplication(): Application = {
    new GuiceApplicationBuilder()
      .configure(Map(
        "mongodb.uri" -> "mongodb://localhost:27017/testGithubTutorial",
        "github.token" -> "test-token"
      ))
      .build()
  }

  override lazy val connector: GitHubConnector = app.injector.instanceOf[GitHubConnector]

  val testUser: User = User(
    login = "big boy",
    name = Some("jason"),
    avatar_url = "http://msn.com/avatar.jpg",
    location = Some("London"),
    bio = Some("i love donuts."),
    followers = 50,
    following = 250,
    created_at = "1955-01-01"
  )

  implicit val userFormat: OFormat[User] = Json.format[User]
  implicit val createOrUpdateFormat: OFormat[CreateOrUpdate] = Json.format[CreateOrUpdate]
  implicit val deleteFormat: OFormat[Delete] = Json.format[Delete]

  "GitHubConnector" should {

    "handle 200 response correctly for GET request" in {
      val url = "/users/username"
      val expectedUrl = s"http://localhost:${wireMockServer.port()}$url"

      wireMockServer.stubFor(get(urlEqualTo(url))
        .willReturn(aResponse()
          .withStatus(OK)
          .withBody(Json.toJson(testUser).toString())))

      whenReady(connector.get[User](expectedUrl).value) { result =>
        result shouldBe Right(testUser)
      }
    }

    "handle non-200 response correctly for GET request" in {
      val url = "/users/username"
      val expectedUrl = s"http://localhost:${wireMockServer.port()}$url"

      wireMockServer.stubFor(get(urlEqualTo(url))
        .willReturn(aResponse()
          .withStatus(NOT_FOUND)
          .withStatusMessage("Not Found")))

      whenReady(connector.get[User](expectedUrl).value) { result =>
        result shouldBe Left(APIError.BadAPIResponse(NOT_FOUND, "Not Found"))
      }
    }

    "handle network error correctly for GET request" in {
      val url = "/users/username"
      val expectedUrl = s"http://localhost:${wireMockServer.port()}$url"

      wireMockServer.stop()

      whenReady(connector.get[User](expectedUrl).value) {
        case Left(error: APIError.BadAPIResponse) =>
          error.upstreamStatus shouldBe 500
          error.upstreamMessage should include("Could not connect to API")
        case _ => fail("Expected a BadAPIResponse error but got something else.")
      }
    }
  }

  override def afterAll(): Unit = {
    wireMockServer.stop()
    super.afterAll()
  }
}
