
package controllers

import models.daos.BPMNDiagramDAO
import scaldi.Injector

import scala.concurrent.Future

/**
  * Controller that handels the help page of the application
  *
  * Created by dennis.benner on 18.09.2016.
  */
class HelpController(implicit inj: Injector) extends ApplicationController {

  /**
    * Get endpoints that returns the help page
    */
  def getHelp = silhouette.SecuredAction.async { implicit request =>
    Future.successful(
      render {
        case Accepts.Html() => Ok(views.html.help(request.identity))
      }
    )
  }
}
