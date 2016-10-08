package forms

import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json

/**
  * Definition of the Sign in Form
  *
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/5/2016
  */
object SignUpForm {

  /**
    * Defines a mapping from and to the Data class
    */
  val form = Form(
    mapping(
      "firstName" -> nonEmptyText,
      "lastName" -> nonEmptyText,
      "email" -> email,
      "password" -> nonEmptyText
    )(Data.apply)(Data.unapply)
  )

  /**
    * Data of the sign up form
    * @param firstName user first name
    * @param lastName user last name
    * @param email user email
    * @param password user password
    */
  case class Data(firstName: String,
                  lastName: String,
                  email: String,
                  password: String)

//  object Data {
//    implicit val jsonFormat = Json.format[Data]
//  }
}

