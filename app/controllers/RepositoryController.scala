package controllers

import models.BPMNDiagram
import models.daos.BPMNDiagramDAO
import scaldi.Injector
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.Future

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/17/2016
  */
class RepositoryController(implicit inj: Injector) extends ApplicationController {
  val diagramDAO = inject[BPMNDiagramDAO]

  //TODO infinity scroll; fetch only first x diagrams
  def repository = silhouette.UserAwareAction.async { implicit request =>
    Future.successful(
      request.identity match {
        case Some(identity) => Ok(views.html.bpmnRepository("Welcome", Some(identity)))
        case None => Ok(views.html.landing(""))
      })
  }

  def repositoryJson = silhouette.SecuredAction.async { implicit request =>
    val repository = for {
      owns <- diagramDAO.listOwns(request.identity.id)
      edits <- diagramDAO.listCanEdit(request.identity.id)
      views <- diagramDAO.listCanView(request.identity.id)
    } yield (owns ::: edits ::: views).distinct

    repository.map({
      list => Ok(Json.obj("diagrams" -> Json.toJson(list.map(BPMNDiagram.toData))))
    })
  }

}
