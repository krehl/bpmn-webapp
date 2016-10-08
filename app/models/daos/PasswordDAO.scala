package models.daos

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json._
import reactivemongo.play.json.BSONFormats._
import reactivemongo.play.json.collection.JSONCollection
import scaldi.{Injectable, Injector}

import scala.collection.mutable
import scala.concurrent.Future

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/6/2016
  */

sealed trait PasswordDAO extends DAO[LoginInfo, PasswordInfo]

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

class MongoPasswordDAO(implicit inj: Injector) extends DelegableAuthInfoDAO[PasswordInfo]
  with Injectable {
  val mongoApi: ReactiveMongoApi = inject[ReactiveMongoApi]

  //this makes life way easier since case classes can be automatically transformed to json
  // -> no need for complicated queries with projections that return only a parts of the document (the authInfo part)
  case class PersistenceWrapper(loginInfo: LoginInfo, authInfo: PasswordInfo)

  implicit val jsonPasswordFormat = Json.format[PasswordInfo]
  implicit val jsonLoginFormat = Json.format[LoginInfo]
  implicit val jsonPersistenceWrapperFormat = Json.format[PersistenceWrapper]


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