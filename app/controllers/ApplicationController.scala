package controllers

import forms.SignUpForm
import util.DefaultEnv
import com.mohiva.play.silhouette.api.Silhouette
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import scaldi.{Injectable, Injector}

import scala.concurrent.Future

class ApplicationController(implicit inj: Injector) extends Controller with Injectable with I18nSupport {
  val messagesApi = inject[MessagesApi]
  val silhouette = inject[Silhouette[DefaultEnv]]

  /**
    * HTTP GET endpoint, presents different content depending on user authentication status
    * @return HTTP OK status with HTML
    */
  def index = silhouette.UserAwareAction.async { implicit request =>
    Future.successful(
      request.identity match {
        case Some(identity) => Ok(views.html.bpmnModeler(s"Hello ${identity.firstName}!", Some(identity)))
        case None => Ok(views.html.signUp(SignUpForm.form, None))
      })
  }
}