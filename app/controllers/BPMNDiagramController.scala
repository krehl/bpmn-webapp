package controllers

import com.mohiva.play.silhouette.api.actions.SecuredRequest
import daos.{BPMNDiagramDAO, InMemoryBPMNDiagramDAO, MongoDBUtil}
import models.BPMNDiagram
import play.api.i18n.Messages
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, Result}
import scaldi.Injector
import util.DefaultEnv
import util.Types._

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.xml.NodeSeq

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 15/07/2016
  */
class BPMNDiagramController(implicit inj: Injector) extends ApplicationController
  with MongoDBUtil {
  //TODO write a monad transformer for user authorization
  //TODO json for everything; think about xml integration
  //TODO read diagram name from request
  val diagramDAO = inject[BPMNDiagramDAO]

  /**
    * Creates a new diagram and returns a HTML page
    *
    * @return
    */
  def newBPMNDiagram = silhouette.SecuredAction.async { implicit request =>
    val newDiagram = BPMNDiagram(
      name = "",
      xmlContent = BPMNDiagram.default,
      owner = request.identity.id,
      canView = Set.empty[UserID],
      canEdit = Set.empty[UserID]
    )
    diagramDAO.save(newDiagram).map({
      case true => Redirect(routes.BPMNDiagramController.loadModeller(newDiagram.id.toString))
      case false => BadRequest("ID already exists!")
    })
  }

  /**
    * Returns a HTML page
    *
    * @param id id
    * @return HTML
    */
  def loadModeller(id: String) = silhouette.SecuredAction.async { implicit request =>
    //    render {//TODO make content negotiation working
    //      case Accepts.Html() => Ok(views.html.bpmnModeller(s"Hello ${request.identity.firstName}!", Some(request.identity), id))
    //      case Accepts.Json() => retrieve3(id)
    //    }
    Future.successful {
      Ok(views.html.bpmnModeller(s"Hello ${request.identity.firstName}!", Some(request.identity), id))
    }
  }

  //------------------------------------------------------------------------------------------//
  // CRUD Operations
  //------------------------------------------------------------------------------------------//
  /**
    * Creates a new diagram resource
    *
    * @return HTTP response depending on success
    */
  def create: Action[NodeSeq] = silhouette.SecuredAction.async(parse.xml) { implicit request =>
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
          "xml" -> newDiagram.xmlContent.toString())
        Ok(json)
      case false => BadRequest("ID already exists!")
    })
  }

  /**
    * Retrieves a diagram with the given id
    *
    * @param id database id
    * @return HTTP response depending on success
    */
  def retrieve(id: String): Action[AnyContent] = silhouette.SecuredAction.async {
    implicit request =>
      ObjectID(id) match {
        case Success(identifier) =>
          diagramDAO.find(identifier).flatMap({
            case Some(diagram) => checkRetrieveAuthorization(identifier, diagram)
            case None => Future.successful(BadRequest(Json.obj("message" -> Messages("bpmn.id.exists"))))
          })
        case Failure(ex) => Future.successful(BadRequest(Json.obj("message" -> Messages("bpmn.id.invalid.format"))))
      }
  }

  /**
    * Checks if user is authorized
    *
    * @param id      id
    * @param diagram diagram
    * @param request HTTP request
    * @return future depending on success
    */
  private[this] def checkRetrieveAuthorization(id: BPMNDiagramID, diagram: BPMNDiagram)
                                              (implicit request: SecuredRequest[DefaultEnv, AnyContent]): Future[Result] = {
    val requestUserID = request.identity.id
    val authorizedUsers = diagram.canView + diagram.owner
    val isAuthorized = authorizedUsers.contains(requestUserID)
    isAuthorized match {
      case true =>
        val json: JsValue = Json.obj(
          "id" -> id.toString,
          "xml" -> diagram.xmlContent.toString())
        Future.successful(Ok(json))
      case false => Future.successful(BadRequest(Json.obj("message" -> Messages("bpmn.diagram.delete.not.authorized"))))
    }
  }

  /**
    * Updates the diagram resource at the given id
    *
    * @param id database id
    * @return xml
    */
  def update(id: String): Action[NodeSeq] = silhouette.SecuredAction.async(parse.xml) {
    implicit request =>
      ObjectID(id) match {
        case Success(identifier) =>
          diagramDAO.find(identifier).flatMap({
            case Some(diagram) => checkUpdateAuthorization(diagram.id.toString, diagram)
            case None => Future.successful(BadRequest("ID not found!"))
          })
        case Failure(ex) => Future.successful(BadRequest(ex.getLocalizedMessage))
      }
  }

  /**
    * Checks if user is authorized
    *
    * @param id      id
    * @param diagram diagram
    * @param request HTTP request
    * @return future depending on success
    */
  private[this] def checkUpdateAuthorization(id: String, diagram: BPMNDiagram)
                                            (implicit request: SecuredRequest[DefaultEnv, NodeSeq]): Future[Result] = {
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

  /**
    * Deletes the diagram resource at the given id
    *
    * @param id database id
    * @return json
    */
  def delete(id: String): Action[String] = silhouette.SecuredAction.async(parse.text) {
    implicit request =>
      ObjectID(id) match {
        case Success(identifier) =>
          diagramDAO.find(identifier).flatMap({
            case Some(diagram) => checkDeleteAuthorization(identifier, diagram)
            case None => Future.successful(BadRequest("ID not found!"))
          })
        case Failure(ex) => Future.successful(BadRequest(ex.getLocalizedMessage))
      }
  }

  def list = silhouette.SecuredAction.async { implicit request =>
    val diagrams = InMemoryBPMNDiagramDAO.bpmnDiagrams map {
      case (k, v) => k.toString
    }
    val diagramsList = diagrams.toList
    Future.successful(Ok(Json.toJson(diagramsList)))
  }

  /**
    * Checks if user is authorized
    *
    * @param id      id
    * @param diagram diagram
    * @param request HTTP request
    * @return future depending on success
    */
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
}