package models.daos

import com.mohiva.play.silhouette.api.LoginInfo
import models.User
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{JsArray, Json}
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
  /**
    * Query by email
    *
    * @param key email
    * @return Future of some user or none
    */
  def findByEmail(key: Email): Future[Option[User]]

  /**
    * Query by id
    *
    * @param key user id
    * @return Future of some user or none
    */
  def findById(key: UserID): Future[Option[User]]

  /**
    * Checks if email does exist
    *
    * @param key email
    * @return Future of boolean true if email exists false otherwise
    */
  def exists(key: Email): Future[Boolean]

  /**
    *
    * @param list List of user ids
    * @return Future list of users
    */
  def findAllById(list: List[UserID]): Future[List[User]]

  /**
    *
    * @param list List of user emails
    * @return Future list of users
    */
  def findAllByEmail(list: List[Email]): Future[List[User]]

}

/**
  *
  * @param inj scaldi injector
  */
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


  override def findById(key: UserID): Future[Option[User]] = {
    for {
      collection <- collection
      dataOption <- collection
        .find(Json.obj("id" -> BSONObjectIDFormat.writes(key)))
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


  def findAllById(list: List[UserID]): Future[List[User]] = {
    val query = Json.obj("id" -> Json.obj("$in" -> JsArray(list.map(BSONObjectIDFormat.writes))))

    for {
      collection <- collection
      data <- collection
        .find(query)
        .cursor[User.Data]()
        .collect[List]()
    } yield data.map(User(_))
  }


  override def findAllByEmail(list: List[Email]): Future[List[User]] = {
    val query = Json.obj("email" -> Json.obj("$in" -> JsArray(list.map(Json.toJson(_)))))

    for {
      collection <- collection
      data <- collection
        .find(query)
        .cursor[User.Data]()
        .collect[List]()
    } yield data.map(User(_))
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