package controllers

import daos.BPMNDiagramDAO
import scaldi.Injector
import play.api.libs.concurrent.Execution.Implicits._

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/17/2016
  */
class RepositoryController(implicit inj: Injector) extends ApplicationController {
  val diagramDAO = inject[BPMNDiagramDAO]


  //  def list2 = silhouette.SecuredAction.async { implicit request =>
  //
  //    diagramDAO.allKeys.flatMap(keySet => Future.successful(Ok(Json.toJson(keySet.toList))))
  //  }
  //  def repository = silhouette.UserAwareAction.async { implicit request =>
  //      request.identity match {
  //        case Some(identity) =>
  //          diagramDAO.allKeys.map(keySet => Ok(views.html.bpmnRepository("Welcome", Some(identity), keySet.toList)))
  //        case None => Future.successful(Redirect(routes.SignInController.view()))
  //      }
  //
  //  }
  //
  //  def list = silhouette.SecuredAction.async { implicit request =>
  //    diagramDAO.allKeys.map(keySet => Ok(Json.toJson(keySet.toList.map(_.toString))))
  //  }

  //TODO infinity scroll; fetch only first x diagrams
  def repository = silhouette.SecuredAction.async { implicit request =>
    diagramDAO.list(request.identity.id).map({
      list => Ok(views.html.bpmnRepository("Welcome", Some(request.identity), list))
    })
  }
}
