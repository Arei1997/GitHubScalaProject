package model

import play.api.libs.json.{Json, OFormat}

case class Repository(
                       name: String,
                       html_url: String,
                       description: Option[String]
                     )

object Repository {
  implicit val format: OFormat[Repository] = Json.format[Repository]
}