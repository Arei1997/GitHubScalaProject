package model

import play.api.libs.json.{Json, OFormat}

case class User(
                 login: String,
                 name: Option[String],          // User's name (optional)
                 avatar_url: String,            // URL to the user's avatar image
                 location: Option[String],      // User's location (optional)
                 bio: Option[String],           // User's bio (optional)
                 followers: Int,                // Number of followers
                 following: Int,                // Number of users the user is following
                 created_at: String             // Account creation date
               )

object User {
  implicit val format: OFormat[User] = Json.format[User]
}
