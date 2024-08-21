import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import cats.data.EitherT
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.typesafe.config.ConfigFactory
import model.{APIError, Contents, CreateOrUpdate, Delete}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.libs.json.Json
import play.api.libs.ws.ahc.AhcWSClient
import connector.GitHubConnector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GitHubConnectorSpec extends PlaySpec with ScalaFutures {

  private val wireMockPort = 9000
  private val wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().port(wireMockPort))

  def beforeAll(): Unit = {
    wireMockRule.start()
  }

  def afterAll(): Unit = {
    wireMockRule.stop()
  }
  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: Materializer = ActorMaterializer()

  private val wsClient = AhcWSClient()

  private val config = Configuration(ConfigFactory.parseString(
    """
      |github.token = "test_token"
      |""".stripMargin))

  private val gitHubConnector = new GitHubConnector(wsClient, config)

  "GitHubConnector#get" should {

    "return valid response when API call is successful" in {
      val responseJson =
        """
          |{
          | "name": "file.txt",
          | "type": "file",
          | "html_url": "https://github.com/user/repo/file.txt",
          | "url": "https://api.github.com/repos/user/repo/contents/file.txt",
          | "path": "file.txt",
          | "sha": "abcdef1234567890",
          | "content": "SGVsbG8gd29ybGQ="
          |}
          |""".stripMargin

      stubFor(get(urlEqualTo("/repos/user/repo/contents/file.txt"))
        .willReturn(aResponse().withStatus(200).withBody(responseJson)))

      val result: EitherT[Future, APIError, Contents] = gitHubConnector.get[Contents](s"http://localhost:$wireMockPort/repos/user/repo/contents/file.txt")

      whenReady(result.value) { res =>
        res mustBe Right(Json.parse(responseJson).as[Contents])
      }
    }

    "return an error when API call fails" in {
      stubFor(get(urlEqualTo("/repos/user/repo/contents/file.txt"))
        .willReturn(aResponse().withStatus(404).withBody("Not Found")))

      val result: EitherT[Future, APIError, Contents] = gitHubConnector.get[Contents](s"http://localhost:$wireMockPort/repos/user/repo/contents/file.txt")

      whenReady(result.value) { res =>
        res mustBe Left(APIError.BadAPIResponse(404, "Not Found"))
      }
    }
  }

  "GitHubConnector#createOrUpdate" should {

    "return valid response when API call is successful" in {
      val requestJson =
        """
          |{
          | "message": "Update file.txt",
          | "content": "SGVsbG8gd29ybGQ=",
          | "sha": "abcdef1234567890"
          |}
          |""".stripMargin

      val responseJson =
        """
          |{
          | "content": {
          |   "name": "file.txt",
          |   "path": "file.txt",
          |   "sha": "abcdef1234567890"
          | }
          |}
          |""".stripMargin

      stubFor(put(urlEqualTo("/repos/user/repo/contents/file.txt"))
        .withRequestBody(equalToJson(requestJson))
        .willReturn(aResponse().withStatus(200).withBody(responseJson)))

      val data = CreateOrUpdate("Update file.txt", "SGVsbG8gd29ybGQ=", Some("abcdef1234567890"))
      val result: EitherT[Future, APIError, Contents] = gitHubConnector.createOrUpdate[Contents](
        s"http://localhost:$wireMockPort/repos/user/repo/contents/file.txt",
        data
      )

      whenReady(result.value) { res =>
        res mustBe Right((Json.parse(responseJson) \ "content").as[Contents])
      }
    }

    "return an error when API call fails" in {
      stubFor(put(urlEqualTo("/repos/user/repo/contents/file.txt"))
        .willReturn(aResponse().withStatus(500).withBody("Internal Server Error")))

      val data = CreateOrUpdate("Update file.txt", "SGVsbG8gd29ybGQ=", Some("abcdef1234567890"))
      val result: EitherT[Future, APIError, Contents] = gitHubConnector.createOrUpdate[Contents](
        s"http://localhost:$wireMockPort/repos/user/repo/contents/file.txt",
        data
      )

      whenReady(result.value) { res =>
        res mustBe Left(APIError.BadAPIResponse(500, "Internal Server Error"))
      }
    }
  }

  "GitHubConnector#delete" should {

    "return valid response when API call is successful" in {
      val requestJson =
        """
          |{
          | "message": "Delete file.txt",
          | "sha": "abcdef1234567890"
          |}
          |""".stripMargin

      val responseJson =
        """
          |{
          | "content": {
          |   "name": "file.txt",
          |   "path": "file.txt",
          |   "sha": "abcdef1234567890"
          | }
          |}
          |""".stripMargin

      stubFor(delete(urlEqualTo("/repos/user/repo/contents/file.txt"))
        .withRequestBody(equalToJson(requestJson))
        .willReturn(aResponse().withStatus(200).withBody(responseJson)))

      val data = Delete("Delete file.txt", "abcdef1234567890")
      val result: EitherT[Future, APIError, Contents] = gitHubConnector.delete[Contents](
        s"http://localhost:$wireMockPort/repos/user/repo/contents/file.txt",
        data
      )

      whenReady(result.value) { res =>
        res mustBe Right((Json.parse(responseJson) \ "content").as[Contents])
      }
    }

    "return an error when API call fails" in {
      stubFor(delete(urlEqualTo("/repos/user/repo/contents/file.txt"))
        .willReturn(aResponse().withStatus(500).withBody("Internal Server Error")))

      val data = Delete("Delete file.txt", "abcdef1234567890")
      val result: EitherT[Future, APIError, Contents] = gitHubConnector.delete[Contents](
        s"http://localhost:$wireMockPort/repos/user/repo/contents/file.txt",
        data
      )

      whenReady(result.value) { res =>
        res mustBe Left(APIError.BadAPIResponse(500, "Internal Server Error"))
      }
    }
  }
}
