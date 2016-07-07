package daos

import com.mohiva.play.silhouette.api.LoginInfo
import models.User

import scala.collection.mutable
import scala.concurrent.Future

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/4/2016
  */
sealed trait UserDAO extends DAO[LoginInfo, User]


class InMemoryUserDAO extends UserDAO {

  import daos.InMemoryUserDAO._

  override def save(user: User): Future[Boolean] = {
    Future.successful({
      if (users.contains(user.loginInfo)) {
        false
      } else {
        users.put(user.loginInfo, user)
        true
      }
    })
  }

  override def update(loginInfo: LoginInfo, user: User): Future[Boolean] = {
    Future.successful({
      if (users.contains(loginInfo)) {
        users.put(loginInfo, user)
        true
      } else {
        false
      }
    })
  }

  override def remove(loginInfo: LoginInfo): Future[Boolean] = {
    Future.successful({
      if (users.contains(loginInfo)) {
        users.remove(loginInfo)
        true
      } else {
        false
      }
    })
  }

  override def find(loginInfo: LoginInfo): Future[Option[User]] = {
    Future.successful(users.get(loginInfo))
  }
}

object InMemoryUserDAO {
  val users: mutable.HashMap[LoginInfo, User] = mutable.HashMap()
}