package model

import play.api.libs.json.{Json, OFormat}

case class CreateOrUpdate(
                           message: String,  // The commit message
                           content: String, // The new file content, using Base64 encoding
                           sha: Option[String]
                         )

object CreateOrUpdate {
  implicit val format: OFormat[CreateOrUpdate] = Json.format[CreateOrUpdate]
}
