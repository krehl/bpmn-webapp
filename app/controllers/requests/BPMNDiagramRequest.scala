package controllers.requests

import com.mohiva.play.silhouette.api.actions.SecuredRequest
import models.{BPMNDiagram, User}
import play.api.mvc.{Request, WrappedRequest}
import util.DefaultEnv

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/18/2016
  */

case class BPMNDiagramRequest[A](diagram: BPMNDiagram, request: SecuredRequest[DefaultEnv, A])
  extends WrappedRequest[A](request) {
  def user = request.identity
}



