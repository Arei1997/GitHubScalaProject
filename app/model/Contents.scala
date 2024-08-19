package model

import play.api.libs.json.{Json, OFormat}

case class Contents(
                     name: String,
                     `type`: String,
                     html_url: String,
                     url: String,
                     path: String,
                     sha: String,              // Add the sha field
                     content: Option[String]   // `content` field is optional, only present for files
                   )

object Contents {
  implicit val format: OFormat[Contents] = Json.format[Contents]
}
