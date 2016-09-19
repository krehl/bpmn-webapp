package controllers.requests

import com.mohiva.play.silhouette.api.actions.SecuredRequest
import models.BPMNDiagram
import play.api.mvc.WrappedRequest
import util.DefaultEnv

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/18/2016
  */

case class BPMNDiagramRequest[A](diagram: BPMNDiagram, request: SecuredRequest[DefaultEnv, A])
  extends WrappedRequest[A](request) {
  def user = request.identity
}



