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
import util.Types.BPMNDiagramID

import scala.concurrent.Future

/**
  * Base Controller class
  *
  * @param inj scaldi injector
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 23/07/2016
  */
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
  /**
    * Composed action that ensures authenticated and authorized access to a BPMNDiagram
    * @param id diagram id
    * @param permission permission that should be ensured (view, edit or owns)
    * @return Action builder
    */
  def DiagramWithPermissionAction(id: BPMNDiagramID, permission: Permission) = {
    silhouette.SecuredAction andThen DiagramAction(id) andThen DiagramPermissionAction(permission)
  }

  //------------------------------------------------------------------------------------------//
  // BASIC APPLICATION ENDPOINTS
  //------------------------------------------------------------------------------------------//
  /**
    * HTTP GET endpoint, presents different content depending on user authentication status;
    * repository view if logged in, landing page otherwise.
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

  //------------------------------------------------------------------------------------------//
  // MISC
  //------------------------------------------------------------------------------------------//
  /**
    * Enables routing for client side javascript code e.g.:
    * $.ajax(jsRoutes.controllers.BPMNDiagramController.update(someId))
    *
    * @return
    */
  def javascriptRoutes = Action { implicit request =>
    Ok(
      JavaScriptReverseRouter("jsRoutes")(
        controllers.routes.javascript.BPMNDiagramController.retrieve,
        controllers.routes.javascript.BPMNDiagramController.update,
        controllers.routes.javascript.BPMNDiagramController.getHistory,
        controllers.routes.javascript.BPMNDiagramController.addPermissions,
        controllers.routes.javascript.BPMNDiagramController.listPermissions,
        controllers.routes.javascript.BPMNDiagramController.removePermissions,
        controllers.routes.javascript.BPMNDiagramController.delete,
        controllers.routes.javascript.RepositoryController.repository,
        controllers.routes.javascript.ProfileController.getProfile,
        controllers.routes.javascript.ProfileController.profile,
        controllers.routes.javascript.RepositoryController.repositoryJson,
        controllers.routes.javascript.BPMNDiagramController.download,
        controllers.routes.javascript.BPMNDiagramController.loadModeller
      )
    ).as("text/javascript")
  }
}