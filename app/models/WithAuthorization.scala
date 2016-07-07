package models

import com.mohiva.play.silhouette.api.Authorization
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import play.api.Logger
import play.api.mvc.Request
import scaldi.{Injectable, Injector}

import scala.concurrent.Future

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/3/2016
  */

sealed abstract case class Role(subSet: Set[Role])

object Customer extends Role(subSet = Set.empty)

object Admin extends Role(subSet = Set(Customer))

case class WithAuthorization(role: Role)(implicit inj: Injector) extends Authorization[User, JWTAuthenticator] with Injectable {

  def isAuthorized[B](user: User, authenticator: JWTAuthenticator)(implicit request: Request[B]) = {
    Logger.info(s"isAuthorized? User = $user; Authenticator = $authenticator")
    Future.successful(user.roles.exists(userRole => userRole == role || userRole.subSet.contains(role)))
  }
}

