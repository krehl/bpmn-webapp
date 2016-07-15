package controllers

import daos.BPMNDiagramDAO
import models.BPMNDiagram
import play.api.Configuration
import scaldi.Injector
import services.UserService
import util.Types._

import scala.concurrent.Future

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 15/07/2016
  */
class BPMNDiagramController(implicit inj: Injector) extends ApplicationController {
  val userService = inject[UserService]
  val bpmnDiagramDAO = inject[BPMNDiagramDAO]
  val configuration = inject[Configuration]

  def save = silhouette.SecuredAction.async(parse.xml) { implicit request =>
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

}
