package model

import play.api.libs.json.{Json, OFormat}

case class Commit(
                   sha: String,
                   commit: CommitDetails
                 )

case class CommitDetails(
                          author: CommitAuthor,
                          committer: CommitAuthor,
                          message: String
                        )

case class CommitAuthor(
                         name: String,
                         email: String,
                         date: String
                       )

object Commit {
  implicit val commitAuthorFormat: OFormat[CommitAuthor] = Json.format[CommitAuthor]
  implicit val commitDetailsFormat: OFormat[CommitDetails] = Json.format[CommitDetails]
  implicit val commitFormat: OFormat[Commit] = Json.format[Commit]
}
