package models

import com.mohiva.play.silhouette.api.Authorization
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import play.api.Logger
import play.api.mvc.Request

import scala.concurrent.Future

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/18/2016
  */

sealed abstract case class Role(subSet: Set[Role])

object Customer extends Role(subSet = Set.empty)

object Admin extends Role(subSet = Set(Customer))

case class WithRole(role: Role) extends Authorization[User, CookieAuthenticator] {

  override def isAuthorized[B](user: User, authenticator: CookieAuthenticator)(implicit request: Request[B]) = {
    Logger.info(s"isAuthorized? role = $role; user = ${user.email}")
    Future.successful(user.roles.exists(userRole => userRole == role || userRole.subSet.contains(role)))
  }
}