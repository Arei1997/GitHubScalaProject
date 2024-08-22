package model

import play.api.libs.json.{Json, OFormat}

case class CreateOrUpdate(
                           message: String,
                           content: String,
                           sha: Option[String]
                         )

object CreateOrUpdate {
  implicit val format: OFormat[CreateOrUpdate] = Json.format[CreateOrUpdate]
}
