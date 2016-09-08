package models.daos

import models.BPMNDiagram
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoApi
import play.modules.reactivemongo.json._
import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat
import reactivemongo.play.json.collection.JSONCollection
import scaldi.{Injectable, Injector}
import util.Types.{BPMNDiagramID, UserID}

import scala.collection.mutable
import scala.concurrent.Future

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 15/07/2016
  */
sealed trait BPMNDiagramDAO extends DAO[BPMNDiagramID, BPMNDiagram] {

  def listCanEdit(userId: UserID): Future[List[BPMNDiagram]]

  def listCanView(userId: UserID): Future[List[BPMNDiagram]]

  def listOwns(userId: UserID): Future[List[BPMNDiagram]]

  //  def canEdit(userId: UserID, diagramId: BPMNDiagramID): Future[Boolean]
  //
  //  def canView(userId: UserID, diagramId: BPMNDiagramID): Future[Boolean]
  //
  //  def owns(userId: UserID, diagramId: BPMNDiagramID): Future[Boolean]


}

class InMemoryBPMNDiagramDAO extends BPMNDiagramDAO {

  import InMemoryBPMNDiagramDAO._


  override def listOwns(key: UserID): Future[List[BPMNDiagram]] = {
    Future.successful(bpmnDiagrams.filter(_._2.owner == key).values.toList)
  }

  override def listCanEdit(key: UserID): Future[List[BPMNDiagram]] = {
    Future.successful(bpmnDiagrams.filter(_._2.canEdit.contains(key)).values.toList)
  }

  override def listCanView(key: UserID): Future[List[BPMNDiagram]] = {
    Future.successful(bpmnDiagrams.filter(_._2.canView.contains(key)).values.toList)
  }

  //  override def canEdit(userId: UserID, diagramId: BPMNDiagramID): Future[Boolean] = {
  //    Future.successful(bpmnDiagrams.exists(_._2.canEdit.contains(userId)))
  //  }
  //
  //  override def canView(userId: UserID, diagramId: BPMNDiagramID): Future[Boolean] = {
  //    Future.successful(bpmnDiagrams.exists(_._2.canView.contains(userId)))
  //  }
  //
  //  override def owns(userId: UserID, diagramId: BPMNDiagramID): Future[Boolean] = {
  //    Future.successful(bpmnDiagrams.exists(_._2.owner == userId))
  //  }

  //------------------------------------------------------------------------------------------//
  // CRUD OPERATIONS
  //------------------------------------------------------------------------------------------//
  /**
    * @param value value
    *
    * @return False if diagram was already present, true otherwise.
    */
  override def save(value: BPMNDiagram): Future[Boolean] = {
    Future.successful({
      if (bpmnDiagrams.contains(value.id)) {
        false
      } else {
        bpmnDiagrams.put(value.id, value)
        true
      }
    })
  }

  /**
    *
    * @param value value
    *
    * @return False if diagram was present, true otherwise.
    */
  override def update(value: BPMNDiagram): Future[Boolean] = {
    Future.successful({
      if (bpmnDiagrams.contains(value.id)) {
        bpmnDiagrams.put(value.id, value)
        true
      } else {
        false
      }
    })
  }

  /**
    *
    * @param key value
    *
    * @return False if diagram was not present, true otherwise.
    */
  override def remove(key: BPMNDiagramID): Future[Boolean] = {
    Future.successful({
      if (bpmnDiagrams.contains(key)) {
        bpmnDiagrams.remove(key)
        true
      } else {
        false
      }
    })
  }

  /**
    *
    * @param key key
    *
    * @return None if diagram is not present, some search result otherwise.
    */
  override def find(key: BPMNDiagramID): Future[Option[BPMNDiagram]] = {
    Future.successful(bpmnDiagrams.get(key))
  }
}

object InMemoryBPMNDiagramDAO {
  val bpmnDiagrams: mutable.HashMap[BPMNDiagramID, BPMNDiagram] = mutable.HashMap()
}

class MongoBPMNDiagramDAO(implicit inj: Injector) extends BPMNDiagramDAO
  with Injectable {
  val mongoApi = inject[ReactiveMongoApi]

  def collection: Future[JSONCollection] = {
    mongoApi.database.map(_.collection[JSONCollection]("diagram"))
  }

  override def listCanEdit(userId: UserID): Future[List[BPMNDiagram]] = {
    val query = Json.obj("canEdit" -> Json.obj("$in" ->  Json.obj("$oid" -> userId.stringify)))
    for {
      collection <- collection
      result <- collection.find(query).cursor[BPMNDiagram]().collect[List]()
    } yield result
  }

  override def listCanView(userId: UserID): Future[List[BPMNDiagram]] = {
    val query = Json.obj("canView" -> Json.obj("$in" ->  Json.obj("$oid" -> userId.stringify)))
    for {
      collection <- collection
      result <- collection.find(query).cursor[BPMNDiagram]().collect[List]()
    } yield result
  }

  override def listOwns(userId: UserID): Future[List[BPMNDiagram]] = {
    val query = Json.obj("owner" -> Json.obj("$oid" -> userId.stringify))
    for {
      collection <- collection
      result <- collection.find(query).cursor[BPMNDiagram]().collect[List]()
    } yield result
  }


  /**
    * @param value value
    *
    * @return False if value was already present, true otherwise.
    */
  override def save(value: BPMNDiagram): Future[Boolean] = {
    val query = Json.obj("_id" -> value.id.stringify)
    for {
      collection <- collection
      result <- collection.update(query, value, upsert = true)
    } yield result.ok
  }

  /**
    *
    * @param value value
    *
    * @return False if value was present, true otherwise.
    */
  override def update(value: BPMNDiagram): Future[Boolean] = {
    for {
      collection <- collection
      result <- collection.update(Json.obj("_id" -> value.id.stringify),
        value,
        upsert = false)
    } yield result.ok
  }

  /**
    *
    * @param key key
    *
    * @return None if value is not present, some search result otherwise.
    */
  override def find(key: BPMNDiagramID): Future[Option[BPMNDiagram]] = {
    for {
      collection <- collection
      result <- collection
        .find(Json.obj("_id" -> key.stringify))
        .one[BPMNDiagram]
    } yield result
  }

  /**
    *
    * @param key key
    *
    * @return False if value was not present, true otherwise.
    */
  override def remove(key: BPMNDiagramID): Future[Boolean] = {
    for {
      collection <- collection
      result <- collection.remove(Json.obj("_id" -> key.stringify))
    } yield result.ok
  }
}