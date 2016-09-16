package controllers

import com.mohiva.play.silhouette.api.Silhouette
import controllers.actions.{DiagramAction, DiagramPermissionAction}
import models.Permission
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import play.api.routing.JavaScriptReverseRouter
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.bson.BSONObjectID
import scaldi.{Injectable, Injector}
import util.DefaultEnv

import scala.concurrent.Future

class ApplicationController(implicit inj: Injector) extends Controller
  with Injectable
  with I18nSupport
  with MongoController
  with ReactiveMongoComponents{
  val messagesApi = inject[MessagesApi]
  val silhouette = inject[Silhouette[DefaultEnv]]
  val reactiveMongoApi= inject[ReactiveMongoApi]


  //------------------------------------------------------------------------------------------//
  // COMPOSED ACTIONS
  //------------------------------------------------------------------------------------------//
  def DiagramWithPermissionAction(id: BSONObjectID, permission: Permission) = {
    silhouette.SecuredAction andThen DiagramAction(id) andThen DiagramPermissionAction(permission)
  }

  /**
    * HTTP GET endpoint, presents different content depending on user authentication status
    *
    * @return HTTP OK status with HTML
    */
  def index = silhouette.UserAwareAction.async { implicit request =>
    Future.successful(
      request.identity match {
        case Some(identity) => Redirect(routes.RepositoryController.repository())
        case None => Ok(views.html.landing(""))
      })
  }

  def javascriptRoutes = Action { implicit request =>
    Ok(
      JavaScriptReverseRouter("jsRoutes")(
        controllers.routes.javascript.BPMNDiagramController.retrieve,
        controllers.routes.javascript.BPMNDiagramController.update,
/*
        controllers.routes.javascript.BPMNDiagramController.addViewers,
*/
        controllers.routes.javascript.BPMNDiagramController.getHistory,
        controllers.routes.javascript.BPMNDiagramController.addPermissions
      )
    ).as("text/javascript")
  }
}