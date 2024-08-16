package model

import play.api.data.Form
import play.api.data.Forms._

case class FileFormData(message: String, content: String, sha: Option[String])

object FileFormData {
  val form: Form[FileFormData] = Form(
    mapping(
      "message" -> nonEmptyText,
      "content" -> nonEmptyText,
      "sha" -> optional(text)
    )(FileFormData.apply)(FileFormData.unapply)
  )
}

