package controllers

import daos.BPMNDiagramDAO
import models.BPMNDiagram
import org.bson.types.ObjectId
import play.api.Configuration
import scaldi.Injector
import services.UserService
import util.Types._
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 15/07/2016
  */
class BPMNDiagramController(implicit inj: Injector) extends ApplicationController {
  val userService = inject[UserService]
  val bpmnDiagramDAO = inject[BPMNDiagramDAO]
  val configuration = inject[Configuration]

  def save = silhouette.SecuredAction.async(parse.text) { implicit request =>
    val newDiagram = BPMNDiagram(
      name = "",
      xmlContent = request.body,
      owner = request.identity.id,
      canView = Set.empty[UserID],
      canEdit = Set.empty[UserID]
    )

    bpmnDiagramDAO.save(newDiagram)
    Future.successful(Ok(request.body))
  }

  def load(id: String) = silhouette.SecuredAction.async { implicit request =>
    for {
      option <- bpmnDiagramDAO.find(new ObjectId(id))
      futureResult <- option match {
        case Some(bpmnDiagram) => Future.successful(Ok(bpmnDiagram.xmlContent))
        case None => Future.successful(BadRequest("bad request"))
      }
    } yield futureResult
  }

  def newBPMNDiagram = silhouette.SecuredAction.async { implicit request =>
    val newDiagram = BPMNDiagram(
      name = "",
      xmlContent = BPMNDiagram.default,
      owner = request.identity.id,
      canView = Set.empty[UserID],
      canEdit = Set.empty[UserID]
    )

    bpmnDiagramDAO.save(newDiagram)
    Future.successful(Redirect(routes.BPMNDiagramController.bpmn(newDiagram.id.toString)))
  }

  def bpmn(id: String) = silhouette.SecuredAction.async{ implicit request =>
  Future.successful{
    Ok(views.html.bpmnModeler(s"Hello ${request.identity.firstName}!", Some(request.identity), id))
  }
}
}
