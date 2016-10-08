package forms

import play.api.data.Form
import play.api.data.Forms._

/**
  * Definition of the Sign in Form
  *
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/5/2016
  */
object SignInForm {

  /**
    * Defines a mapping from and to the Data class
    */
  val form = Form(
    mapping(
      "email" -> email,
      "password" -> nonEmptyText,
      "rememberMe" -> boolean
    )(Data.apply)(Data.unapply))

  /**
    * Data of the sign in form
    * @param email email
    * @param password password
    * @param rememberMe remember me
    */
  case class Data(email: String,
                  password: String,
                  rememberMe: Boolean)

}
