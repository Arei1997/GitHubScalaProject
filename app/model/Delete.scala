package model

import play.api.libs.json.{Json, OFormat}

case class Delete(
                   message: String,  // The commit message
                   sha: String       // The blob SHA of the file being deleted
                 )

object Delete {
  implicit val format: OFormat[Delete] = Json.format[Delete]
}
