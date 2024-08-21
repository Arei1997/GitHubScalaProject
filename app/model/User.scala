package model

import play.api.libs.json.{Json, OFormat}

case class User(
                 login: String,
                 name: Option[String],
                 avatar_url: String,
                 location: Option[String],
                 bio: Option[String],
                 followers: Int,
                 following: Int,
                 created_at: String
               )

object User {
  implicit val format: OFormat[User] = Json.format[User]
}
