package models.daos

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json._
import reactivemongo.play.json.collection.JSONCollection
import scaldi.{Injectable, Injector}

import scala.concurrent.Future

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/6/2016
  */
sealed trait PasswordDAO extends DAO[LoginInfo, PasswordInfo]

/**
  * DAO that has MongoDB as a backing store
  *
  * @param inj scaldi injector
  */
class MongoPasswordDAO(implicit inj: Injector) extends DelegableAuthInfoDAO[PasswordInfo]
  with Injectable {
  val mongoApi: ReactiveMongoApi = inject[ReactiveMongoApi]


  /**
    * This makes life way easier since case classes can be automatically transformed to json
    * -> no need for complicated queries with projections that return only parts of the document
    * (the authInfo part) in order to enable JSON to object transformation.
    */
  case class PersistenceWrapper(loginInfo: LoginInfo, authInfo: PasswordInfo)

  implicit val jsonPasswordFormat = Json.format[PasswordInfo]
  implicit val jsonLoginFormat = Json.format[LoginInfo]
  implicit val jsonPersistenceWrapperFormat = Json.format[PersistenceWrapper]


  /**
    * Calls the reactive mongo driver and retrieves the password collection
    *
    * @return Future of the collection
    */
  def collection: Future[JSONCollection] = {
    mongoApi.database.map(_.collection[JSONCollection]("password"))
  }

  override def find(loginInfo: LoginInfo): Future[Option[PasswordInfo]] = {
    for {
      collection <- collection
      resultWrapper <- collection.find(Json.obj("loginInfo" -> loginInfo)).one[PersistenceWrapper]
      result <- resultWrapper match {
        case Some(wrapper) => Future.successful(Some(wrapper.authInfo))
        case None => Future.successful(None)
      }
    } yield result
  }


  override def add(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = {
    (for {
      collection <- collection
      result <- collection.insert(PersistenceWrapper(loginInfo, authInfo))
    } yield result).map(result => authInfo)
  }

  override def update(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = {
    (for {
      collection <- collection
      result <- collection.update(
        Json.obj("loginInfo" -> loginInfo),
        PersistenceWrapper(loginInfo, authInfo),
        upsert = false)
    } yield result).map(result => authInfo)
  }

  override def save(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = {
    find(loginInfo).flatMap {
      case Some(_) => update(loginInfo, authInfo)
      case None => add(loginInfo, authInfo)
    }
  }

  override def remove(loginInfo: LoginInfo): Future[Unit] = {
    (for {
      collection <- collection
      result <- collection.remove(Json.obj("loginInfo" -> loginInfo))
    } yield result).map(result => Unit)
  }
}