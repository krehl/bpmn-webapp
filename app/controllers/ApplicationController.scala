package controllers

import com.mohiva.play.silhouette.api.Silhouette
import daos.InMemoryBPMNDiagramDAO
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import play.api.routing.JavaScriptReverseRouter
import scaldi.{Injectable, Injector}
import util.DefaultEnv

import scala.concurrent.Future

class ApplicationController(implicit inj: Injector) extends Controller with Injectable with I18nSupport {
  val messagesApi = inject[MessagesApi]
  val silhouette = inject[Silhouette[DefaultEnv]]

  /**
    * HTTP GET endpoint, presents different content depending on user authentication status
    *
    * @return HTTP OK status with HTML
    */
  def index = silhouette.UserAwareAction.async { implicit request =>
    Future.successful(
      request.identity match {
        case Some(identity) => Ok(views.html.bpmnRepository("Hello", Some(identity), List.empty))
        case None => Redirect(routes.SignInController.view())
      })
  }


  def javascriptRoutes = Action { implicit request =>

    Ok(
      JavaScriptReverseRouter("jsRoutes")(
        controllers.routes.javascript.BPMNDiagramController.retrieve,
        controllers.routes.javascript.BPMNDiagramController.update
      )
    ).as("text/javascript")
  }
}