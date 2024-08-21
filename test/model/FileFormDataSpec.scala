import model.FileFormData
import org.scalatestplus.play.PlaySpec
import play.api.data.Form
import play.api.data.FormError

class FileFormDataSpec extends PlaySpec {

  "FileFormData form" should {

    "bind correctly with valid data" in {
      val data = Map(
        "fileName" -> "test.txt",
        "message" -> "Initial commit",
        "content" -> "SGVsbG8gd29ybGQ=",  // Base64 encoded "Hello world"
        "sha" -> "1234567890abcdef"
      )

      val boundForm = FileFormData.form.bind(data)

      boundForm.errors mustBe empty
      boundForm.value mustBe Some(FileFormData("test.txt", "Initial commit", "SGVsbG8gd29ybGQ=", Some("1234567890abcdef")))
    }

    "fail to bind when fileName is missing" in {
      val data = Map(
        "message" -> "Initial commit",
        "content" -> "SGVsbG8gd29ybGQ=",
        "sha" -> "1234567890abcdef"
      )

      val boundForm = FileFormData.form.bind(data)

      boundForm.hasErrors mustBe true
      boundForm.errors must contain(FormError("fileName", "error.required"))
    }

    "fail to bind when message is missing" in {
      val data = Map(
        "fileName" -> "test.txt",
        "content" -> "SGVsbG8gd29ybGQ=",
        "sha" -> "1234567890abcdef"
      )

      val boundForm = FileFormData.form.bind(data)

      boundForm.hasErrors mustBe true
      boundForm.errors must contain(FormError("message", "error.required"))
    }

    "fail to bind when content is missing" in {
      val data = Map(
        "fileName" -> "test.txt",
        "message" -> "Initial commit",
        "sha" -> "1234567890abcdef"
      )

      val boundForm = FileFormData.form.bind(data)

      boundForm.hasErrors mustBe true
      boundForm.errors must contain(FormError("content", "error.required"))
    }

    "bind successfully with missing sha" in {
      val data = Map(
        "fileName" -> "test.txt",
        "message" -> "Initial commit",
        "content" -> "SGVsbG8gd29ybGQ="
      )

      val boundForm = FileFormData.form.bind(data)

      boundForm.errors mustBe empty
      boundForm.value mustBe Some(FileFormData("test.txt", "Initial commit", "SGVsbG8gd29ybGQ=", None))
    }
  }
}
