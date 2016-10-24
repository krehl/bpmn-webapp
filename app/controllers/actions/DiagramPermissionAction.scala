package controllers.actions

import controllers.requests.BPMNDiagramRequest
import models._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{ActionFilter, Results}
import scaldi.{Injectable, Injector}

import scala.concurrent.Future

/**
  * An ActionFilter that takes a BPMNDiagramRequest and filters out not authorized requests.
  *
  * @param permission permission type that should be checked for authorization (edit, view or owns)
  * @param inj scaldi injector
  *
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/19/2016
  */
case class DiagramPermissionAction(permission: Permission)
                                  (implicit inj: Injector)
  extends ActionFilter[BPMNDiagramRequest]
    with Results
    with Injectable
    with I18nSupport {
  val messagesApi = inject[MessagesApi]

  /**
    * Intercepts the request and returns a result (forbidden) if no permission to access the
    * diagram is found, otherwise the request is further processed.
    *
    * @param input request
    * @tparam A content type
    * @return An optional Result with which to abort the request
    */
  override def filter[A](input: BPMNDiagramRequest[A]) = Future.successful {
    if (isAuthorized(input.user, input.diagram, permission)) {
      None
    } else {
      Some(Forbidden)
    }
  }

  /**
    * Checks if permission is sufficient
    *
    * @param user user
    * @param diagram BPMNDiagram
    * @param permission permission
    * @return true if permission is granted false otherwise
    */
  private[this] def isAuthorized(user: User,
                                 diagram: BPMNDiagram,
                                 permission: Permission): Boolean = {
    val canView = diagram.canView.contains(user.id)
    val canEdit = diagram.canEdit.contains(user.id)
    val owns = user.id == diagram.owner
    permission match {
      case CanView => canView || canEdit || owns
      case CanEdit => canEdit || owns
      case Owns => owns
    }
  }
}

