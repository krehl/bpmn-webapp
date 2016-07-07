package forms

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/5/2016
  */
object SignUpForm {

  val form = Form(
    mapping(
      "firstName" -> nonEmptyText,
      "lastName" -> nonEmptyText,
      "email" -> email,
      "password" -> nonEmptyText
    )(Data.apply)(Data.unapply)
  )

  case class Data(firstName: String,
                  lastName: String,
                  email: String,
                  password: String)

  object Data {

    implicit val jsonFormat = Json.format[Data]
  }

}

