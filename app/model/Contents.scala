package model

import play.api.libs.json.{Json, OFormat}

case class Contents(
                     name: String,
                     `type`: String,
                     html_url: String
                   )

object Contents {
  implicit val format: OFormat[Contents] = Json.format[Contents]
}
