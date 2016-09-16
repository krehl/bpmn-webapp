package forms

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import util.Types.Email

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/5/2016
  */
object PermissionForm {

  val form = Form(
    mapping(
      "emails" -> list(text),
      "permission" -> nonEmptyText
    )(Data.apply)(Data.unapply)
  )

  case class Data(emails: List[Email], permission: String)

  object Data {

    implicit val jsonFormat = Json.format[Data]
  }

}
