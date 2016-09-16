package models.daos

import com.mohiva.play.silhouette.api.LoginInfo
import models.User
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json._
import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat
import reactivemongo.play.json.collection.JSONCollection
import scaldi.{Injectable, Injector}
import util.Types._

import scala.concurrent.Future

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/4/2016
  */
sealed trait UserDAO extends DAO[LoginInfo, User] {
  def findByEmail(key: Email): Future[Option[User]]

  def exists(key: Email): Future[Boolean]

  def getAll(list: List[UserID]): Future[List[User]]
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
      dataOption <- collection
        .find(Json.obj("email" -> key))
        .one[User.Data]
    } yield dataOption.map(User(_))
  }

  override def exists(key: Email): Future[Boolean] = {
    for {
      collection <- collection
      result <- collection
        .count(Some(Json.obj("email" -> key)))
    } yield result > 0
  }


  def getAll(list: List[UserID]): Future[List[User]] = {
  /*  val query = Json.obj("id" -> Json.obj("$in" -> list))

    for {
      collection <- collection
      dataOption <- collection
        .find(query)
        .one[User.Data]
    } yield dataOption.map(User(_))*/
    ???
  }


  /**
    * @param value value
    * @return False if value was already present, true otherwise.
    */
  override def save(value: User): Future[Boolean] = {
    for {
      collection <- collection
      writeResult <- collection.update(Json.obj("_id" -> value.id.stringify),
        User.toData(value),
        upsert = true)
    } yield writeResult.ok
  }

  /**
    *
    * @param value value
    * @return False if value was present, true otherwise.
    */
  override def update(value: User): Future[Boolean] = {
    for {
      collection <- collection
      writeResult <- collection.update(Json.obj("_id" -> value.id.stringify),
        User.toData(value),
        upsert = false)
    } yield writeResult.ok
  }

  /**
    *
    * @param key key
    * @return None if value is not present, some search result otherwise.
    */
  override def find(key: LoginInfo): Future[Option[User]] = {
    for {
      collection <- collection
      dataOption <- collection
        .find(Json.obj("loginInfo" -> key))
        .one[User.Data]
    } yield dataOption.map(User(_))
  }

  /**
    *
    * @param key key
    * @return False if value was not present, true otherwise.
    */
  override def remove(key: LoginInfo): Future[Boolean] = {
    for {
      collection <- collection
      writeResult <- collection.remove(Json.obj("loginInfo" -> key))
    } yield writeResult.ok
  }
}