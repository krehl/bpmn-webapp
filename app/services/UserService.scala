package services

import models.daos.UserDAO
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import models.User
import scaldi.{Injectable, Injector}

import scala.concurrent.Future

/**
  * Service that is required by Silhouette, acts as a layer between DAO and Controller
  *
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/28/2016
  */
sealed trait UserService extends IdentityService[User] {
  override def retrieve(loginInfo: LoginInfo): Future[Option[User]]

  def save(user: User): Future[Boolean]
}

/**
  * Service implementation
  *
  * @param inj scladi injector
  */
class UserIdentityService(implicit inj: Injector) extends UserService with Injectable {
  val userDAO = inject[UserDAO]

  override def retrieve(loginInfo: LoginInfo): Future[Option[User]] = userDAO.find(loginInfo)

  override def save(user: User): Future[Boolean] = userDAO.save(user)
}
