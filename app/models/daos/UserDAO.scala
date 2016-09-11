package models.daos

import com.mohiva.play.silhouette.api.LoginInfo
import models.{Customer, Role, User}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json._
import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat
import reactivemongo.play.json.collection.JSONCollection
import scaldi.{Injectable, Injector}
import util.Types.Email

import scala.collection.mutable
import scala.concurrent.Future

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/4/2016
  */
sealed trait UserDAO extends DAO[LoginInfo, User] {
  def findByEmail(key: Email): Future[Option[User]]

  def exists(key: Email): Future[Boolean]
}


class InMemoryUserDAO extends UserDAO {

  import InMemoryUserDAO._

  override def findByEmail(key: Email): Future[Option[User]] = ???

  override def exists(key: Email): Future[Boolean] = ???

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

  override def update(user: User): Future[Boolean] = {
    Future.successful({
      if (users.contains(user.loginInfo)) {
        users.put(user.loginInfo, user)
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
  val dummyUser = User(
    loginInfo = LoginInfo("credentials", "1@1"),
    firstName = "1",
    lastName = "1",
    email = "1@1",
    roles = Set[Role](Customer))
  users.put(dummyUser.loginInfo, dummyUser)
}

class MongoUserDAO(implicit inj: Injector) extends UserDAO
  with Injectable {
  val mongoApi = inject[ReactiveMongoApi]

  def collection: Future[JSONCollection] = {
    mongoApi.database.map(_.collection[JSONCollection]("user"))
  }

  override def findByEmail(key: Email): Future[Option[User]] = {
    for {
      collection <- collection
      result <- collection
        .find(Json.obj("email" -> key))
        .one[User]
    } yield result
  }

  override def exists(key: Email): Future[Boolean] = {
    for {
      collection <- collection
      result <- collection
        .count(Some(Json.obj("email" -> key)))
    } yield result > 0
  }


  /**
    * @param value value
    *
    * @return False if value was already present, true otherwise.
    */
  override def save(value: User): Future[Boolean] = {
    for {
      collection <- collection
      writeResult <- collection.update(Json.obj("_id" -> value.id.stringify),
        value,
        upsert = true)
    } yield writeResult.ok
  }

  /**
    *
    * @param value value
    *
    * @return False if value was present, true otherwise.
    */
  override def update(value: User): Future[Boolean] = {
    for {
      collection <- collection
      writeResult <- collection.update(Json.obj("_id" -> value.id.stringify),
        value,
        upsert = false)
    } yield writeResult.ok
  }

  /**
    *
    * @param key key
    *
    * @return None if value is not present, some search result otherwise.
    */
  override def find(key: LoginInfo): Future[Option[User]] = {
    for {
      collection <- collection
      result <- collection
        .find(Json.obj("loginInfo" -> key))
        .one[User]
    } yield result
  }

  /**
    *
    * @param key key
    *
    * @return False if value was not present, true otherwise.
    */
  override def remove(key: LoginInfo): Future[Boolean] = {
    for {
      collection <- collection
      writeResult <- collection.remove(Json.obj("loginInfo" -> key))
    } yield writeResult.ok
  }
}