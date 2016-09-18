
package controllers

import models.daos.BPMNDiagramDAO
import scaldi.Injector

import scala.concurrent.Future

/**
  * Created by dennis.benner on 18.09.2016.
  */
class HelpController(implicit inj: Injector) extends ApplicationController {
  val diagramDAO = inject[BPMNDiagramDAO]

  def getHelp = silhouette.SecuredAction.async { implicit request =>
    Future.successful(
      render {
        case Accepts.Html() => Ok(views.html.help(request.identity))
      }
    )
  }
}
