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
  * Controller that manages access of an BPMNDiagram. The controller not only provides CRUD but
  * also more fine grained methods.
  *
  * @param inj scaldi injector
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 15/07/2016
  */
class BPMNDiagramController(implicit inj: Injector) extends ApplicationController {
  val diagramDAO = inject[BPMNDiagramDAO]
  val userDAO = inject[UserDAO]

  /**
    * Creates a new default diagram, persists it and returns a HTML page which views the created
    * diagram
    *
    * @return Either InternalServerError or HTML
    */
  def newBPMNDiagram = silhouette.SecuredAction.async {
    implicit request =>
      val newDiagram = BPMNDiagram(
        BPMNDiagram.Data(
          name = Messages("bpmn.default.title"),
          description = Messages("bpmn.default.description"),
          timeStamp = Instant.now(),
          owner = request.identity.id,
          editor = request.identity.id,
          canView = Set.empty[UserID],
          canEdit = Set.empty[UserID]
        )
      )
      diagramDAO.save(newDiagram).map({
        case true => Redirect(routes.BPMNDiagramController.loadModeller(newDiagram.id))
        case false => InternalServerError
      })
  }

  /**
    * Returns a HTML page which views the passed diagram, authorization is handled when the
    * actual diagram information is requested.
    *
    * @param id diagram id
    * @return HTML depending on sign in status
    */
  def loadModeller(id: BPMNDiagramID) = silhouette.UserAwareAction.async { implicit request =>
    Future.successful(
      request.identity match {
        case Some(identity) => Ok(views.html.bpmnModeller(s"Hello ${identity.firstName}!", request.identity, id))
        case None => Redirect(routes.SignInController.view())
      })
  }

  /**
    * Downloads a diagram with the given id
    *
    * @param id database id
    * @return HTTP response depending on authorization (can view) success
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

  /**
    * JSON that consists of two lists, one for view and one for edit, which contain the
    * email addresses of users with the corresponding permission. Requires owner permissions.
    *
    * @param id diagram id
    * @return HTTP OK with the JSON result
    */
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

  /**
    * Adds permissions to a diagram, expects user emails as JSON lists (one for edit one for view).
    * Requires owner permissions.
    *
    * @param id diagram id
    * @return HTTP response depending on success
    */
  def addPermissions(id: BPMNDiagramID) = DiagramWithPermissionAction(id, Owns).async(parse.json) {
    implicit request =>
      val json = request.body
      val diagram = request.diagram
      val dbResult = editPermissions(id, json, diagram.owner, dbFunction = diagram.addPermissions)

      dbResult.map({
        case true => Ok
        case false => BadRequest("Diagram not found!")
      })
  }

  /**
    * Removes permissions to a diagram, expects user emails as JSON lists (one for edit one for
    * view).
    * Requires owner permissions.
    *
    * @param id diagram id
    * @return HTTP response depending on success
    */
  def removePermissions(id: BPMNDiagramID) = DiagramWithPermissionAction(id, Owns).async(parse.json) {
    implicit request =>
      val json = request.body
      val diagram = request.diagram
      val dbResult = editPermissions(id, json, diagram.owner, dbFunction = diagram.removePermissions)

      dbResult.map({
        case true => Ok
        case false => BadRequest("Diagram not found!")
      })
  }

  /**
    * Edits the permissions of a diagram
    *
    * @param id         diagram id
    * @param json       json with 2 lists (canView & canEdit) of user emails
    * @param owner      owner id; is needed for internal modelling reasons (resolve in the future?)
    * @param dbFunction takes user lists and returns a future of boolean (success == true)
    * @return result of the call to the db function
    */
  private[this] def editPermissions(id: BPMNDiagramID,
                                    json: JsValue,
                                    owner: UserID,
                                    dbFunction: (List[UserID], List[UserID]) => Future[Boolean]) = {
    /**
      * Extracts a list of users from the json
      *
      * @param key name of the json list
      * @return list of users
      */
    def getJsonAsEmailList(key: String): Future[List[UserID]] = {
      for {
        users <- {
          val emails = (json \ key).as[List[Email]]
          userDAO.findAllByEmail(emails)
        }
      } yield users.map(_.id).filter(_ != owner)
    }

    val viewers: Future[List[UserID]] = getJsonAsEmailList("canView")

    val editors: Future[List[UserID]] = getJsonAsEmailList("canEdit")

    for {
      // we need list but got future[list]
      // so we transform it into Future[List[List]] and flatmap it
      permissions <- Future.sequence(List(viewers, editors))
      //dbFunction(viewers,editors)
      result <- dbFunction(permissions.head, permissions(1))
    } yield result
  }

  /**
    * Retrieves the entire change history of a diagram
    *
    * @param id diagram id
    * @return JSON in HTTP response depending on success
    */
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
    * @return JSON in  HTTP response depending on success
    */
  def create = silhouette.SecuredAction.async {
    implicit request =>
      val newDiagram = BPMNDiagram(
        BPMNDiagram.Data(
          name = Messages("bpmn.default.title"),
          description = Messages("bpmn.default.description"),
          timeStamp = Instant.now(),
          owner = request.identity.id,
          editor = request.identity.id,
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
    * @return HTTP response depending on success
    */
  def update(id: BPMNDiagramID) = DiagramWithPermissionAction(id, CanEdit).async(parse.json) {
    implicit request =>
      val json = request.body

      //extract fields
      val name = (json \ "name").as[String]
      val description = (json \ "description").as[String]
      val xmlContent = scala.xml.XML.loadString((json \ "xmlContent").as[String])

      //create diagram object
      val diagram = BPMNDiagram(
        BPMNDiagram.toData(request.diagram).copy(
          name = name,
          description = description,
          xmlContent = xmlContent,
          editor = request.user.id)
      )

      //update database
      diagramDAO.save(diagram).map({
        case true => Ok
        case false => InternalServerError
      })
  }

  /**
    * Deletes the diagram resource at the given id
    *
    * @param id database id
    * @return HTTP response depending on success
    */
  def delete(id: BPMNDiagramID) = DiagramWithPermissionAction(id, Owns).async {
    implicit request =>
      diagramDAO.remove(request.diagram.id).map({
        case true => Ok
        case false => InternalServerError
      })
  }
}