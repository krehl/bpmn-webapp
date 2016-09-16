package controllers

import models.User
import models.daos.BPMNDiagramDAO
import play.api.libs.json.Json
import scaldi.Injector

import scala.concurrent.Future

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 16/09/2016
  */
class ProfileController(implicit inj: Injector) extends ApplicationController {
  val diagramDAO = inject[BPMNDiagramDAO]

  def getProfile = silhouette.SecuredAction.async { implicit request =>
    Future.successful(
      render {
        case Accepts.Html() => Ok(views.html.profile(request.identity))
        case Accepts.Json() => Ok(Json.toJson(User.toData(request.identity)))
      }
    )
  }
}
