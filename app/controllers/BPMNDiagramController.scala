package controllers

import java.time.Instant

import models._
import models.daos.{BPMNDiagramDAO, UserDAO}
import play.api.i18n.Messages
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsValue, Json}
import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat
import scaldi.Injector
import util.Types.{BPMNDiagramID, Email, UserID}

import scala.concurrent.Future

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 15/07/2016
  */
class BPMNDiagramController(implicit inj: Injector) extends ApplicationController {
  val diagramDAO = inject[BPMNDiagramDAO]
  val userDAO = inject[UserDAO]

  /**
    * Creates a new diagram and returns a HTML page
    *
    * @return
    */
  def newBPMNDiagram = silhouette.SecuredAction.async {
    implicit request =>
      val newDiagram = BPMNDiagram(
        BPMNDiagram.Data(
          name = Messages("bpmn.default.title"),
          description = Messages("bpmn.default.description"),
          timeStamp = Instant.now(),
          owner = request.identity.id,
          canView = Set.empty[UserID],
          canEdit = Set.empty[UserID]
        )
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
      //      case Accepts.Html() => Ok(views.html.bpmnModeller(s"Hello ${request.identity.firstName}!", Some(request
      // .identity), id))
      //      case Accepts.Json() => retrieve3(id)
      //    }

      Future.successful {
        Ok(views.html.bpmnModeller(s"Hello ${request.identity.firstName}!",
          Some(request.identity),
          id)
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
                .concat(request.diagram.name)
                .concat(".bpmn"))
          .as("application/x-download")
      })
  }

  def listPermissions(id: BPMNDiagramID) = DiagramWithPermissionAction(id, Owns).async {
    implicit request =>
      (for {
        viewerEmails <- request.diagram.listUserThatCanView.map(_.map(_.email))
        editorEmails <- request.diagram.listUserThatCanEdit.map(_.map(_.email))
      } yield Json.obj(
        "canView" -> Json.toJson(viewerEmails),
        "canEdit" -> Json.toJson(editorEmails)
      )).map(Ok(_))
  }

  def addPermissions(id: BPMNDiagramID) = DiagramWithPermissionAction(id, Owns).async(parse.json) {
    implicit request =>
      val json = request.body
      val diagram = request.diagram
      //transforms List[Future[Option[User]]] into Future[List[Option[User]]] into Future[List[User]]
      // into Future[List[UserID]]
      val viewers: Future[List[UserID]] = Future.sequence(
        (json \ "canView").as[List[Email]].filter(_ != diagram.owner).map(userDAO.findByEmail)
      ).map(_.flatten.map(_.id))

      val editors: Future[List[UserID]] = Future.sequence(
        (json \ "canEdit").as[List[Email]].filter(_ != diagram.owner).map(userDAO.findByEmail)
      ).map(_.flatten.map(_.id))

      //little hacky
      (for {
        permissions <- Future.sequence(List(viewers, editors)).map(list => (list.head, list(1)))
        result <- diagram.addPermissions(permissions._1, permissions._2)
      } yield result).map({
        case true => Ok
        case false => BadRequest("Diagram not found!")
      })

  }

  def removePermissions(id: BPMNDiagramID) = DiagramWithPermissionAction(id, Owns).async(parse.json) {
    implicit request =>
      val json = request.body
      val diagram = request.diagram
      val viewers = (json \ "canView").as[List[UserID]]
      val editors = (json \ "canEdit").as[List[UserID]]

      diagram.removePermissions(viewers, editors).map({
        case true => Ok
        case false => BadRequest("Diagram not found!")
      })
  }

  def getHistory(id: BPMNDiagramID) = DiagramWithPermissionAction(id, CanView).async {
    implicit request =>
      diagramDAO.findHistory(id).map({
        case list: List[BPMNDiagram] => Ok(Json.toJson(list.map(BPMNDiagram.toData)))
        case _ => InternalServerError
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
  def create = silhouette.SecuredAction.async {
    implicit request =>
      val newDiagram = BPMNDiagram(
        BPMNDiagram.Data(
          name = Messages("bpmn.default.title"),
          description = Messages("bpmn.default.description"),
          timeStamp = Instant.now(),
          owner = request.identity.id,
          canView = Set.empty[UserID],
          canEdit = Set.empty[UserID]
        )
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
        val json: JsValue = Json.toJson(BPMNDiagram.toData(diagram))
        Ok(json)
      })
  }

  /**
    * Updates the diagram resource at the given id
    *
    * @param id database id
    * @return xml
    */
  def update(id: BPMNDiagramID) = DiagramWithPermissionAction(id, CanEdit).async(parse.json) {
    implicit request =>
      val json = request.body
      val name = (json \ "name").as[String]
      val description = (json \ "description").as[String]
      val xmlContent = scala.xml.XML.loadString((json \ "xmlContent").as[String])
      val diagram = BPMNDiagram(
        BPMNDiagram.toData(request.diagram).copy(
          name = name,
          description = description,
          xmlContent = xmlContent)
      )

      diagramDAO.save(diagram).map({
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