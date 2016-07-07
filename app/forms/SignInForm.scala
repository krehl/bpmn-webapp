package forms

import play.api.data.Form
import play.api.data.Forms._

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/5/2016
  */
object SignInForm {

  val form = Form(
    mapping(
      "email" -> email,
      "password" -> nonEmptyText,
      "rememberMe" -> boolean
    )(Data.apply)(Data.unapply))

  case class Data(email: String,
                  password: String,
                  rememberMe: Boolean)

}
