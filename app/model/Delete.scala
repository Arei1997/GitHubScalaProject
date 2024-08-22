package model

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{Json, OFormat}

case class Delete(message: String, sha: String)

object Delete {
  implicit val format: OFormat[Delete] = Json.format[Delete]

  val form: Form[Delete] = Form(
    mapping(
      "message" -> nonEmptyText,
      "sha" -> nonEmptyText
    )(Delete.apply)(Delete.unapply)
  )
}
