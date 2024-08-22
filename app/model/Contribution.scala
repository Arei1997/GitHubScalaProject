package model

import play.api.libs.json.{Json, OFormat}

case class Contribution(date: String, count: Int)

object Contribution {
  // This defines the JSON format for the Contribution class
  implicit val format: OFormat[Contribution] = Json.format[Contribution]
}
