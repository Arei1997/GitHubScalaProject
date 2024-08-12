package model

import play.api.libs.json.{Json, OFormat}

case class User(
                 login: String,
                 created_at: String,
                 location: Option[String],
                 followers: Int,
                 following: Int
               )

object User {
  implicit val format: OFormat[User] = Json.format[User]
}


//curl command  CREATED-- {"login":"testName","created_at":"today","location":"London","followers":780033,"following":2}%
//UPDATE curl -X PUT -H "Content-Type: application/json" -d '{"login":"testName","created_at":"today","location":"London","followers":780033,"following":10}' http://localhost:9000/api/testName
//DELETE curl -X DELETE http://localhost:9000/api/testName
