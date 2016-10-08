package controllers

import com.mohiva.play.silhouette.api.LogoutEvent
import scaldi.Injector

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/7/2016
  */
class SignOutController(implicit inj: Injector) extends ApplicationController {

  /**
    * HTTP POST endpoint, requires a signed in user
    * @return Redirect to the application main page
    */
  def signOut = silhouette.SecuredAction.async { implicit request =>
    silhouette.env.eventBus.publish(LogoutEvent(request.identity, request))
    silhouette.env.authenticatorService.discard(request.authenticator, Redirect(routes.ApplicationController.index()))
  }
}
