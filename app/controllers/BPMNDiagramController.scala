package controllers

import com.mohiva.play.silhouette.api.actions.SecuredRequest
import daos.{BPMNDiagramDAO, MongoDBUtil}
import models.BPMNDiagram
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, Result}
import scaldi.Injector
import util.DefaultEnv
import util.Types._

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 15/07/2016
  */
class BPMNDiagramController(implicit inj: Injector) extends ApplicationController
  with MongoDBUtil {
  val diagramDAO = inject[BPMNDiagramDAO]

  def newBPMNDiagram = silhouette.SecuredAction.async { implicit request =>
    val newDiagram = BPMNDiagram(
      name = "",
      xmlContent = BPMNDiagram.default,
      owner = request.identity.id,
      canView = Set.empty[UserID],
      canEdit = Set.empty[UserID]
    )

    diagramDAO.save(newDiagram).map({
      case true => Redirect(routes.BPMNDiagramController.retrieve(newDiagram.id.toString))
      case false => BadRequest("ID already exists!")
    })
  }

  //------------------------------------------------------------------------------------------//
  // CRUD Operations
  //------------------------------------------------------------------------------------------//

  def create = silhouette.SecuredAction.async(parse.text) { implicit request =>
    val newDiagram = BPMNDiagram(
      name = "",
      xmlContent = request.body,
      owner = request.identity.id,
      canView = Set.empty[UserID],
      canEdit = Set.empty[UserID]
    )

    diagramDAO.save(newDiagram).map({
      case true =>
        val json: JsValue = Json.obj(
          "id" -> newDiagram.id.toString,
          "xml" -> newDiagram.xmlContent)
        Ok(json)
      case false => BadRequest("ID already exists!")
    })
  }

  def retrieve(id: String): Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    ObjectID(id) match {
      case Success(identifier) =>
        diagramDAO.find(identifier).flatMap({
          case Some(diagram) => checkRetrieveAuthorization(identifier, diagram)
          case None => Future.successful(BadRequest("ID not found!"))
        })
      case Failure(ex) => Future.successful(BadRequest("Invalid ID format!"))
    }
  }

  private[this] def checkRetrieveAuthorization(id: BPMNDiagramID, diagram: BPMNDiagram)
                                              (implicit request: SecuredRequest[DefaultEnv, AnyContent]): Future[Result] = {
    val requestUserID = request.identity.id
    val authorizedUsers = diagram.canView + diagram.owner
    val isAuthorized = authorizedUsers.contains(requestUserID)
    isAuthorized match {
      case true =>
        val json: JsValue = Json.obj(
          "id" -> id.toString,
          "xml" -> diagram.xmlContent)
        Future.successful(Ok(json))
      case false => Future.successful(BadRequest("You are not authorized to delete this diagram!"))
    }
  }

  def update(id: String): Action[String] = silhouette.SecuredAction.async(parse.text) { implicit request =>
    ObjectID(id) match {
      case Success(identifier) =>
        diagramDAO.find(identifier).flatMap({
          case Some(diagram) => checkUpdateAuthorization(diagram.id.toString, diagram)
          case None => Future.successful(BadRequest("ID not found!"))
        })
      case Failure(ex) => Future.successful(BadRequest(ex.getLocalizedMessage))
    }
  }

  private[this] def checkUpdateAuthorization(id: String, diagram: BPMNDiagram)
                                            (implicit request: SecuredRequest[DefaultEnv, String]): Future[Result] = {
    val requestUserID = request.identity.id
    val authorizedUsers = diagram.canEdit + diagram.owner
    val isAuthorized = authorizedUsers.contains(requestUserID)
    isAuthorized match {
      case true =>
        diagramDAO.update(diagram.copy(xmlContent = request.body)).map({
          case true => Ok("Update successful!")
          case false => BadRequest("Internal server error, ID not found!")
        })
      case false => Future.successful(BadRequest("You are not authorized to update this diagram!"))
    }
  }

  def delete(id: String): Action[String] = silhouette.SecuredAction.async(parse.text) { implicit request =>
    ObjectID(id) match {
      case Success(identifier) =>
        diagramDAO.find(identifier).flatMap({
          case Some(diagram) => checkDeleteAuthorization(identifier, diagram)
          case None => Future.successful(BadRequest("ID not found!"))
        })
      case Failure(ex) => Future.successful(BadRequest(ex.getLocalizedMessage))
    }
  }

  private[this] def checkDeleteAuthorization(id: BPMNDiagramID, diagram: BPMNDiagram)
                                            (implicit request: SecuredRequest[DefaultEnv, String]): Future[Result] = {
    val isAuthorized = request.identity.id == diagram.owner
    isAuthorized match {
      case true =>
        diagramDAO.remove(id).map({
          case true => Ok("Delete successful!")
          case false => BadRequest("Internal server error, ID not found!")
        })
      case false => Future.successful(BadRequest("You are not authorized to delete this diagram!"))
    }
  }


  //  def getBPMNDiagram(id: String) = silhouette.SecuredAction.async { implicit request =>
  //    Future.successful {
  //      Ok(views.html.bpmnModeler(s"Hello ${request.identity.firstName}!", Some(request.identity), id))
  //    }
  //  }
}