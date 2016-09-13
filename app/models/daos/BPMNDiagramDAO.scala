package models.daos

import java.time.Instant

import _root_.util.Types.{UserID, _}
import models.BPMNDiagram
import play.api.libs.concurrent.Execution.Implicits._
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

//class InMemoryBPMNDiagramDAO extends BPMNDiagramDAO {
//
//  import InMemoryBPMNDiagramDAO._
//
//
//  override def listOwns(key: UserID): Future[List[BPMNDiagram]] = {
//    Future.successful(bpmnDiagrams.filter(_._2.owner == key).values.toList)
//  }
//
//  override def listCanEdit(key: UserID): Future[List[BPMNDiagram]] = {
//    Future.successful(bpmnDiagrams.filter(_._2.canEdit.contains(key)).values.toList)
//  }
//
//  override def listCanView(key: UserID): Future[List[BPMNDiagram]] = {
//    Future.successful(bpmnDiagrams.filter(_._2.canView.contains(key)).values.toList)
//  }
//
//
//  override def removeViewers(key: BPMNDiagramID, viewers: List[UserID]): Future[Boolean] = ???
//
//  override def removeEditors(key: BPMNDiagramID, viewers: List[UserID]): Future[Boolean] = ???
//
//  override def findHistory(key: BPMNDiagramID): Future[List[BPMNDiagram]] = ???
//
//  override def addEditors(key: BPMNDiagramID, editors: List[UserID]): Future[Boolean] = ???
//
//  override def addViewers(key: BPMNDiagramID, viewers: List[UserID]): Future[Boolean] = ???
//
//  //------------------------------------------------------------------------------------------//
//  // CRUD OPERATIONS
//  //------------------------------------------------------------------------------------------//
//  /**
//    * @param value value
//    * @return False if diagram was already present, true otherwise.
//    */
//  override def save(value: BPMNDiagram): Future[Boolean] = {
//    Future.successful({
//      if (bpmnDiagrams.contains(value.id)) {
//        false
//      } else {
//        bpmnDiagrams.put(value.id, value)
//        true
//      }
//    })
//  }
//
//  /**
//    *
//    * @param value value
//    * @return False if diagram was present, true otherwise.
//    */
//  override def update(value: BPMNDiagram): Future[Boolean] = {
//    Future.successful({
//      if (bpmnDiagrams.contains(value.id)) {
//        bpmnDiagrams.put(value.id, value)
//        true
//      } else {
//        false
//      }
//    })
//  }
//
//  /**
//    *
//    * @param key value
//    * @return False if diagram was not present, true otherwise.
//    */
//  override def remove(key: BPMNDiagramID): Future[Boolean] = {
//    Future.successful({
//      if (bpmnDiagrams.contains(key)) {
//        bpmnDiagrams.remove(key)
//        true
//      } else {
//        false
//      }
//    })
//  }
//
//  /**
//    *
//    * @param key key
//    * @return None if diagram is not present, some search result otherwise.
//    */
//  override def find(key: BPMNDiagramID): Future[Option[BPMNDiagram]] = {
//    Future.successful(bpmnDiagrams.get(key))
//  }
//}
//
//object InMemoryBPMNDiagramDAO {
//  val bpmnDiagrams: mutable.HashMap[BPMNDiagramID, BPMNDiagram] = mutable.HashMap()
//}

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
    } yield data.map(BPMNDiagram(_)).groupBy(_.id).map(_._2.head).toList
    //dirty trick to ensure only 1 result per diagram and not 1 for every version
  }

  override def listCanView(userId: UserID): Future[List[BPMNDiagram]] = {
    val query = Json.obj("canView" -> Json.obj("$in" -> Json.arr(Json.obj("$oid" -> userId.stringify))))
    for {
      collection <- collection
      data <- collection.
        find(query)
        .cursor[BPMNDiagram.Data]()
        .collect[List]()
    } yield data.map(BPMNDiagram(_)).groupBy(_.id).map(_._2.head).toList
    //dirty trick to ensure only 1 result per diagram and not 1 for every version
  }

  override def listOwns(userId: UserID): Future[List[BPMNDiagram]] = {
    val query = Json.obj("owner" -> Json.obj("$oid" -> userId.stringify))
    for {
      collection <- collection
      data <- collection
        .find(query)
        .sort(Json.obj("timeStamp" -> -1))
        .cursor[BPMNDiagram.Data]()
        .collect[List]()
    } yield data.map(BPMNDiagram(_)).groupBy(_.id).map(_._2.head).toList
    //dirty trick to ensure only 1 result per diagram and not 1 for every version
  }


  override def addEditors(key: BPMNDiagramID, editors: List[UserID]): Future[Boolean] = {
    //    implicit object UserIDWrites extends Writes[List[UserID]] {
    //      def writes(ids: List[UserID]) = Json.arr(ids.map(id => Json.obj("$oid" -> id.stringify)))
    //    }
    implicit object UserIDWrites extends Writes[UserID] {
      def writes(id: UserID) = Json.obj("$oid" -> id.stringify)
    }

    val query = Json.obj("id" -> BSONObjectIDFormat.writes(key))
    //TODO Json serializing
    val modifier = Json.obj("$addToSet" -> Json.obj("canEdit" -> Json.obj("$each" -> Json.arr(BSONObjectIDFormat.writes(editors.head)))))
    for {
      collection <- collection
      result <- collection.update(query,
        modifier,
        upsert = false,
        multi = true)
    } yield result.ok
  }


  override def addViewers(key: BPMNDiagramID, viewers: List[UserID]): Future[Boolean] = {
    //    implicit object UserIDWrites extends Writes[List[UserID]] {
    //      def writes(ids: List[UserID]) = Json.arr(ids.map(id => Json.obj("$oid" -> id.stringify)))
    //    }

    implicit object UserIDWrites extends Writes[UserID] {
      def writes(id: UserID) = Json.obj("$oid" -> id.stringify)
    }
    val query = Json.obj("id" -> BSONObjectIDFormat.writes(key))
    val modifier = Json.obj("$addToSet" -> Json.obj("canView" -> Json.obj("$each" -> Json.arr(BSONObjectIDFormat.writes(viewers.head)))))
    for {
      collection <- collection
      result <- collection.update(query,
        modifier,
        upsert = false,
        multi = true)
    } yield result.ok
  }


  override def removeViewers(key: BPMNDiagramID, viewers: List[UserID]): Future[Boolean] = {
    //    implicit object UserIDWrites extends Writes[List[UserID]] {
    //      def writes(ids: List[UserID]) = Json.arr(ids.map(id => Json.obj("$oid" -> id.stringify)))
    //    }

    implicit object UserIDWrites extends Writes[UserID] {
      def writes(id: UserID) = Json.obj("$oid" -> id.stringify)
    }

    val query = Json.obj("id" -> BSONObjectIDFormat.writes(key))
    val modifier = Json.obj("$pullAll" -> Json.obj("canView" -> Json.arr(BSONObjectIDFormat.writes(viewers.head))))
    for {
      collection <- collection
      result <- collection.update(query,
        modifier,
        upsert = false,
        multi = true)
    } yield result.ok
  }

  override def removeEditors(key: BPMNDiagramID, viewers: List[UserID]): Future[Boolean] = {
    //    implicit object UserIDWrites extends Writes[List[UserID]] {
    //      def writes(ids: List[UserID]) = Json.arr(ids.map(id => Json.obj("$oid" -> id.stringify)))
    //    }

    implicit object UserIDWrites extends Writes[UserID] {
      def writes(id: UserID) = Json.obj("$oid" -> id.stringify)
    }

    val query = Json.obj("id" -> BSONObjectIDFormat.writes(key))
    val modifier = Json.obj("$pullAll" -> Json.obj("canEdit" -> Json.arr(BSONObjectIDFormat.writes(viewers.head))))
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
    //    val query = Json.obj()
    for {
      collection <- collection
      result <- collection.insert(
          BPMNDiagram.toData(value).copy(timeStamp = Instant.now())
      )
    //      result <- collection.update(query, value.copy(timeStamp = Instant.now()), upsert = true)
    } yield result.ok
  }

  /**
    *
    * @param value value
    * @return False if value was present, true otherwise.
    */
  override def update(value: BPMNDiagram): Future[Boolean] = {
    val query = Json.obj("id" -> BSONObjectIDFormat.writes(value.id))
    //      Json.obj("$oid" -> value.id.stringify))
    //    val modifier = > db.docs.update( {_id: doc._id}, { $set : { text : 'New Text' }, $push : { hist : doc.text
    // } } )
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