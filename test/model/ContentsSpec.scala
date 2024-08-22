package model
import model.Contents
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsSuccess, Json}

class ContentsSpec extends PlaySpec {

  "Contents model" should {

    "serialize to JSON correctly" in {
      val contents = Contents(
        name = "file.txt",
        `type` = "file",
        html_url = "https://github.com/user/repo/file.txt",
        url = "https://api.github.com/repos/user/repo/contents/file.txt",
        path = "file.txt",
        sha = Some("abcdef1234567890"),
        content = Some("SGVsbG8gd29ybGQ=")
      )

      val json = Json.toJson(contents)
      val expectedJson = Json.parse(
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
          |""".stripMargin)

      json mustBe expectedJson
    }

    "deserialize from JSON correctly" in {
      val json = Json.parse(
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
          |""".stripMargin)

      val expectedContents = Contents(
        name = "file.txt",
        `type` = "file",
        html_url = "https://github.com/user/repo/file.txt",
        url = "https://api.github.com/repos/user/repo/contents/file.txt",
        path = "file.txt",
        sha = Some("abcdef1234567890"),
        content = Some("SGVsbG8gd29ybGQ=")
      )

      val result = json.validate[Contents]

      result mustBe JsSuccess(expectedContents)
    }

    "deserialize from JSON with missing optional content correctly" in {
      val json = Json.parse(
        """
          |{
          | "name": "file.txt",
          | "type": "file",
          | "html_url": "https://github.com/user/repo/file.txt",
          | "url": "https://api.github.com/repos/user/repo/contents/file.txt",
          | "path": "file.txt",
          | "sha": "abcdef1234567890"
          |}
          |""".stripMargin)

      val expectedContents = Contents(
        name = "file.txt",
        `type` = "file",
        html_url = "https://github.com/user/repo/file.txt",
        url = "https://api.github.com/repos/user/repo/contents/file.txt",
        path = "file.txt",
        sha = Some("abcdef1234567890"),
        content = None
      )

      val result = json.validate[Contents]

      result mustBe JsSuccess(expectedContents)
    }

    "fail to deserialize from JSON with missing required fields" in {
      val json = Json.parse(
        """
          |{
          | "name": "file.txt",
          | "type": "file"
          |}
          |""".stripMargin)

      val result = json.validate[Contents]

      result mustBe a[JsError]
    }
  }
}
