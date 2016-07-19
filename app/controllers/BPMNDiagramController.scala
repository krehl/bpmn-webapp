package controllers

import daos.{BPMNDiagramDAO, MongoDBUtil}
import models.{BPMNDiagram, CanEdit, CanView, Owns}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsValue, Json}
import scaldi.Injector
import util.Types.UserID

import scala.concurrent.Future

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 15/07/2016
  */
class BPMNDiagramController(implicit inj: Injector) extends ApplicationController
  with MongoDBUtil {
  //TODO json for everything; think about xml integration
  //TODO read diagram name from request
  val diagramDAO = inject[BPMNDiagramDAO]

  /**
    * Creates a new diagram and returns a HTML page
    *
    * @return
    */
  def newBPMNDiagram = silhouette.SecuredAction.async {
    implicit request =>
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
  def loadModeller(id: String) = silhouette.SecuredAction.async {
    implicit request =>
      //    render {//TODO make content negotiation working
      //      case Accepts.Html() => Ok(views.html.bpmnModeller(s"Hello ${request.identity.firstName}!", Some(request.identity), id))
      //      case Accepts.Json() => retrieve3(id)
      //    }
      Future.successful {
        Ok(views.html.bpmnModeller(s"Hello ${
          request.identity.firstName
        }!", Some(request.identity), id))
      }
  }

  /**
    * Downloads a diagram with the given id
    *
    * @param id database id
    * @return HTTP response depending on success
    */
  def download(id: String) = DiagramWithPermissionAction(id, CanView).async {
    implicit request =>
      Future.successful({
        Ok(request.diagram.xmlContent)
          .withHeaders(
            CONTENT_DISPOSITION ->
              "attachment; filename="
                .concat(request.diagram.id.toString)
                .concat(".bpmn"))
          .as("application/x-download")
      })
  }

  //------------------------------------------------------------------------------------------//
  // CRUD Operations
  //------------------------------------------------------------------------------------------//
  /**
    * Creates a new diagram resource
    *
    * @return json HTTP response depending on success
    */
  def create = silhouette.SecuredAction.async(parse.xml) {
    implicit request =>
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
            "name" -> newDiagram.name,
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
  def retrieve(id: String) = DiagramWithPermissionAction(id, CanView).async {
    implicit request =>
      Future.successful({
        val diagram = request.diagram
        val json: JsValue = Json.obj(
          "id" -> id.toString,
          "name" -> diagram.name,
          "xml" -> diagram.xmlContent.toString())
        Ok(json)
      })
  }

  /**
    * Updates the diagram resource at the given id
    *
    * @param id database id
    * @return xml
    */
  def update(id: String) = DiagramWithPermissionAction(id, CanEdit).async(parse.xml) {
    implicit request =>
      diagramDAO.update(request.diagram.copy(xmlContent = request.body)).map({
        case true => Ok
        case false => InternalServerError
      })
  }

  /**
    * Deletes the diagram resource at the given id
    *
    * @param id database id
    * @return json
    */
  def delete(id: String) = DiagramWithPermissionAction(id, Owns).async {
    implicit request =>
      diagramDAO.remove(request.diagram.id).map({
        case true => Ok
        case false => InternalServerError
      })
  }

  /**
    * Downloads a diagram with the given id
    *
    * @param id database id
    * @return HTTP response depending on success
    */
  def download(id: String): Action[AnyContent] = silhouette.SecuredAction.async {
    implicit request =>
      ObjectID(id) match {
        case Success(identifier) =>
          diagramDAO.find(identifier).flatMap({
            case Some(diagram) => Future.successful(Ok(diagram.xmlContent).withHeaders(CONTENT_TYPE -> "application/x-download",
                                                                                        CONTENT_DISPOSITION -> "attachment; filename=".concat(diagram.id.toString).concat(".bpmn")))
            case None => Future.successful(BadRequest(Json.obj("message" -> Messages("bpmn.id.not_exists"))))
          })
        case Failure(ex) => Future.successful(BadRequest(Json.obj("message" -> Messages("bpmn.id.invalid.format"))))
      }
  }

}