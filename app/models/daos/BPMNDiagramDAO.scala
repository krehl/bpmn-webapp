package models.daos

import java.time.Instant

import _root_.util.Types.{UserID, _}
import models.BPMNDiagram
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json._
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json._
import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat
import reactivemongo.play.json.collection.JSONCollection
import scaldi.{Injectable, Injector}

import scala.concurrent.Future

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 15/07/2016
  */
sealed trait BPMNDiagramDAO extends DAO[BPMNDiagramID, BPMNDiagram] {

  def listCanEdit(userId: UserID): Future[List[BPMNDiagram]]

  def listCanView(userId: UserID): Future[List[BPMNDiagram]]

  def listOwns(userId: UserID): Future[List[BPMNDiagram]]

  def findHistory(key: BPMNDiagramID): Future[List[BPMNDiagram]]

  def addEditors(key: BPMNDiagramID, editors: List[UserID]): Future[Boolean]

  def addViewers(key: BPMNDiagramID, viewers: List[UserID]): Future[Boolean]

  def removeViewers(key: BPMNDiagramID, viewers: List[UserID]): Future[Boolean]

  def removeEditors(key: BPMNDiagramID, viewers: List[UserID]): Future[Boolean]


  //  def canEdit(userId: UserID, diagramId: BPMNDiagramID): Future[Boolean]
  //
  //  def canView(userId: UserID, diagramId: BPMNDiagramID): Future[Boolean]
  //
  //  def owns(userId: UserID, diagramId: BPMNDiagramID): Future[Boolean]

}

class MongoBPMNDiagramDAO(implicit inj: Injector) extends BPMNDiagramDAO
  with Injectable {
  val mongoApi = inject[ReactiveMongoApi]

  def collection: Future[JSONCollection] = {
    mongoApi.database.map(_.collection[JSONCollection]("diagram"))
  }

  override def listCanEdit(userId: UserID): Future[List[BPMNDiagram]] = {
    val query = Json.obj("canEdit" -> Json.obj("$in" -> Json.arr(Json.obj("$oid" -> userId.stringify))))
    for {
      collection <- collection
      data <- collection
        .find(query)
        .cursor[BPMNDiagram.Data]()
        .collect[List]()
    } yield data.map(BPMNDiagram(_)).groupBy(_.id).map(_._2.sorted.head).toList
  }

  override def listCanView(userId: UserID): Future[List[BPMNDiagram]] = {
    val query = Json.obj("canView" -> Json.obj("$in" -> Json.arr(Json.obj("$oid" -> userId.stringify))))
    for {
      collection <- collection
      data <- collection.
        find(query)
        .cursor[BPMNDiagram.Data]()
        .collect[List]()
    } yield data.map(BPMNDiagram(_)).groupBy(_.id).map(_._2.sorted.head).toList
  }

  override def listOwns(userId: UserID): Future[List[BPMNDiagram]] = {
    val query = Json.obj("owner" -> Json.obj("$oid" -> userId.stringify))
    for {
      collection <- collection
      data <- collection
        .find(query)
        .cursor[BPMNDiagram.Data]()
        .collect[List]()
    } yield data.map(BPMNDiagram(_)).groupBy(_.id).map(_._2.sorted.head).toList
  }


  override def addViewers(key: BPMNDiagramID, viewers: List[UserID]): Future[Boolean] = {
    val query = Json.obj("id" -> BSONObjectIDFormat.writes(key))
    val modifier = Json.obj(
      "$addToSet" -> Json.obj(
        "canView" -> Json.obj(
          "$each" -> JsArray(viewers.map(BSONObjectIDFormat.writes))
        )
      )
    )

    for {
      collection <- collection
      result <- collection.update(query,
        modifier,
        upsert = false,
        multi = true)
    } yield result.ok
  }


  override def removeViewers(key: BPMNDiagramID, viewers: List[UserID]): Future[Boolean] = {
    val query = Json.obj("id" -> BSONObjectIDFormat.writes(key))
    val modifier = Json.obj(
      "$pullAll" -> Json.obj(
        "canView" -> JsArray(viewers.map(BSONObjectIDFormat.writes))
      )
    )
    
    for {
      collection <- collection
      result <- collection.update(query,
        modifier,
        upsert = false,
        multi = true)
    } yield result.ok
  }

  override def addEditors(key: BPMNDiagramID, editors: List[UserID]): Future[Boolean] = {
    val query = Json.obj("id" -> BSONObjectIDFormat.writes(key))
    val modifier = Json.obj(
      "$addToSet" -> Json.obj(
        "canEdit" -> Json.obj(
          "$each" -> JsArray(editors.map(BSONObjectIDFormat.writes))
        )
      )
    )

    for {
      collection <- collection
      result <- collection.update(query,
        modifier,
        upsert = false,
        multi = true)
    } yield result.ok
  }

  override def removeEditors(key: BPMNDiagramID, editors: List[UserID]): Future[Boolean] = {
    val query = Json.obj("id" -> BSONObjectIDFormat.writes(key))
    //    val modifier = Json.obj("$pullAll" -> Json.obj("canEdit" -> Json.arr(BSONObjectIDFormat.writes(viewers.head))))
    val modifier = Json.obj(
      "$pullAll" -> Json.obj(
        "canEdit" -> JsArray(editors.map(BSONObjectIDFormat.writes))
      )
    )

    for {
      collection <- collection
      result <- collection.update(query,
        modifier,
        upsert = false,
        multi = true)
    } yield result.ok
  }


  /**
    * @param value value
    * @return False if value was already present, true otherwise.
    */
  override def save(value: BPMNDiagram): Future[Boolean] = {
    for {
      collection <- collection
      result <- collection.insert(
        BPMNDiagram.toData(value).copy(timeStamp = Instant.now())
      )
    } yield result.ok
  }

  /**
    *
    * @param value value
    * @return False if value was present, true otherwise.
    */
  override def update(value: BPMNDiagram): Future[Boolean] = {
    val query = Json.obj("id" -> BSONObjectIDFormat.writes(value.id))
    for {
      collection <- collection
      result <- collection.update(query,
        BPMNDiagram.toData(value),
        upsert = false)
    } yield result.ok
  }

  /**
    *
    * @param key key
    * @return None if value is not present, some search result otherwise.
    */
  override def find(key: BPMNDiagramID): Future[Option[BPMNDiagram]] = {
    for {
      collection <- collection
      dataOption <- collection
        .find(Json.obj("id" -> BSONObjectIDFormat.writes(key)))
        .sort(Json.obj("timeStamp" -> -1))
        .one[BPMNDiagram.Data]
    } yield dataOption.map(BPMNDiagram(_))
  }

  override def findHistory(key: BPMNDiagramID): Future[List[BPMNDiagram]] = {
    val query = Json.obj("id" -> BSONObjectIDFormat.writes(key))
    for {
      collection <- collection
      dataOption <- collection.find(query).cursor[BPMNDiagram.Data]().collect[List]()
    } yield dataOption.map(BPMNDiagram(_))
  }

  /**
    *
    * @param key key
    * @return False if value was not present, true otherwise.
    */
  override def remove(key: BPMNDiagramID): Future[Boolean] = {
    for {
      collection <- collection
      result <- collection.remove(Json.obj("id" -> BSONObjectIDFormat.writes(key)))
    } yield result.ok
  }
}