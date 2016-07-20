package controllers

import models.daos.BPMNDiagramDAO
import models.{BPMNDiagram, CanEdit, CanView, Owns}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsValue, Json}
import reactivemongo.bson.BSONObjectID
import scaldi.Injector
import util.Types.{BPMNDiagramID, UserID}

import scala.concurrent.Future

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 15/07/2016
  */
class BPMNDiagramController(implicit inj: Injector) extends ApplicationController {
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
        case true => Redirect(routes.BPMNDiagramController.loadModeller(newDiagram.id))
        case false => BadRequest("ID already exists!")
      })
  }

  /**
    * Returns a HTML page
    *
    * @param id id
    * @return HTML
    */
  def loadModeller(id: BPMNDiagramID) = silhouette.SecuredAction.async {
    implicit request =>
      //    render {//TODO make content negotiation working
      //      case Accepts.Html() => Ok(views.html.bpmnModeller(s"Hello ${request.identity.firstName}!", Some(request.identity), id))
      //      case Accepts.Json() => retrieve3(id)
      //    }

      Future.successful {
        Ok(views.html.bpmnModeller(s"Hello ${ request.identity.firstName}!",
          Some(request.identity),
          id.stringify)
        )
      }
  }

  /**
    * Downloads a diagram with the given id
    *
    * @param id database id
    * @return HTTP response depending on success
    */
  def download(id: BPMNDiagramID) = DiagramWithPermissionAction(id, CanView).async {
    implicit request =>
      Future.successful({
        Ok(request.diagram.xmlContent)
          .withHeaders(
            CONTENT_DISPOSITION ->
              "attachment; filename="
                .concat(request.diagram.id.stringify)
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
            "id" -> newDiagram.id.stringify,
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
  def retrieve(id: BPMNDiagramID) = DiagramWithPermissionAction(id, CanView).async {
    implicit request =>
      Future.successful({
        val diagram = request.diagram
        val json: JsValue = Json.obj(
          "id" -> id.stringify,
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
  def update(id: BPMNDiagramID) = DiagramWithPermissionAction(id, CanEdit).async(parse.xml) {
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
  def delete(id: BPMNDiagramID) = DiagramWithPermissionAction(id, Owns).async {
    implicit request =>
      diagramDAO.remove(request.diagram.id).map({
        case true => Ok
        case false => InternalServerError
      })
  }
}