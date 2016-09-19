package models

import com.mohiva.play.silhouette.api.Authorization
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import play.api.Logger
import play.api.mvc.Request

import scala.concurrent.Future

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/3/2016
  */

sealed trait Permission

object CanView extends Permission

object CanEdit extends Permission

object Owns extends Permission

//case class WithPermission(permission: Permission,
//                          diagram: BPMNDiagram)
//  extends Authorization[User, CookieAuthenticator] {
//
//  override def isAuthorized[B](user: User,
//                               authenticator: CookieAuthenticator)
//                              (implicit request: Request[B]): Future[Boolean] = {
//    Logger.info(s"isAuthorized? permission = $permission; diagram = ${diagram.id}; user = ${user.email}")
//    val result = permission match {
//      case CanEdit => diagram.canEdit.contains(user.id) || diagram.owner == user.id
//      case CanView => diagram.canView.contains(user.id) || diagram.owner == user.id
//      case Owns => diagram.owner == user.id
//    }
//    Future.successful(result)
//  }
//}