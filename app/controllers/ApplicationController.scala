package controllers

import com.mohiva.play.silhouette.api.Silhouette
import forms.SignUpForm
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import scaldi.{Injectable, Injector}
import util.DefaultEnv

import scala.concurrent.Future

class ApplicationController(implicit inj: Injector) extends Controller with Injectable with I18nSupport {
  val messagesApi = inject[MessagesApi]
  val silhouette = inject[Silhouette[DefaultEnv]]

  def index = silhouette.UserAwareAction.async { implicit request =>
    Future.successful(
      request.identity match {
        case Some(identity) => Ok(views.html.index(s"Hello ${identity.firstName}!"))
        case None => Ok(views.html.signUp(SignUpForm.form))
      })
  }
}