package models.daos

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO

import scala.collection._
import scala.concurrent.Future

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/6/2016
  */

sealed trait PasswordDAO extends DAO[LoginInfo, PasswordInfo]

//this is not good...but we ve to implement the library interface here :( ... TODO solution
class InMemoryPasswordDAO extends DelegableAuthInfoDAO[PasswordInfo] {

  import InMemoryPasswordDAO._

  override def find(loginInfo: LoginInfo): Future[Option[PasswordInfo]] = {
    Future.successful(passwords.get(loginInfo))
  }

  override def update(loginInfo: LoginInfo, password: PasswordInfo): Future[PasswordInfo] = {
    Future.successful({
      if (passwords.contains(loginInfo)) {
        passwords.update(loginInfo, password)
        password
      } else {
        password
      }
    })
  }

  override def remove(loginInfo: LoginInfo): Future[Unit] = {
    Future.successful({
      passwords.remove(loginInfo)
      Unit
    })
  }

  override def save(loginInfo: LoginInfo, password: PasswordInfo): Future[PasswordInfo] = {
    if (passwords.contains(loginInfo)) {
      update(loginInfo, password)
    } else {
      add(loginInfo, password)
    }
  }

  override def add(loginInfo: LoginInfo, password: PasswordInfo): Future[PasswordInfo] = {
    Future.successful({
      if (!passwords.contains(loginInfo)) {
        passwords.put(loginInfo, password)
        password
      } else {
        password
      }
    })
  }
}

object InMemoryPasswordDAO {
  val passwords: mutable.HashMap[LoginInfo, PasswordInfo] = mutable.HashMap()
  val dummyPassword = PasswordInfo("bcrypt", "$2a$10$WrYi4ugL45Pnaql.JGGH9O65kUBdyFbtwVdTA5/5vV9EnSRkig/Be", None)
  passwords.put(LoginInfo("credentials", "1@1"), dummyPassword)
}
