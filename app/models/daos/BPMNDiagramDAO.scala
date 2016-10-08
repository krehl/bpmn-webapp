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

  /**
    * List of latest diagrams (latest version) that can by edited by the specified user
    *
    * @param userId user id
    * @return
    */
  def listCanEdit(userId: UserID): Future[List[BPMNDiagram]]

  /**
    * List of latest diagrams (latest version) that can by viewed by the specified user
    *
    * @param userId user id
    * @return
    */
  def listCanView(userId: UserID): Future[List[BPMNDiagram]]

  /**
    * List of latest diagrams (latest version) that are owned by the specified user
    *
    * @param userId user id
    * @return
    */
  def listOwns(userId: UserID): Future[List[BPMNDiagram]]

  /**
    * Returns entire change history of the specified diagram
    *
    * @param key diagram id
    * @return Future list of all diagram versions
    */
  def findHistory(key: BPMNDiagramID): Future[List[BPMNDiagram]]

  /**
    *
    * @param key     diagram id
    * @param viewers list of user ids that should be able to view
    * @param editors ist of user ids that should be able to edit
    * @return future of boolean true if successful false otherwise
    */
  def addPermissions(key: BPMNDiagramID,
                     viewers: List[UserID],
                     editors: List[UserID]): Future[Boolean]

  /**
    *
    * @param key     diagram id
    * @param viewers list of user ids that should no longer be able to view
    * @param editors list of user ids that should no longer be able to edit
    * @return future of boolean true if successful false otherwise
    */
  def removePermissions(key: BPMNDiagramID,
                        viewers: List[UserID],
                        editors: List[UserID]): Future[Boolean]
}

/**
  * DAO that has MongoDB as a backing store
  *
  * Each diagram version is a single document in the database, but different versions of the same
  * diagram have identical ids. An ordering of versions is established by the timeStamp field.
  *
  * @param inj scaldi injector
  */
class MongoBPMNDiagramDAO(implicit inj: Injector) extends BPMNDiagramDAO
  with Injectable {
  val mongoApi = inject[ReactiveMongoApi]

  /**
    * Calls the reactive mongo driver and retrieves the password collection
    *
    * @return Future of the collection
    */
  def collection: Future[JSONCollection] = {
    mongoApi.database.map(_.collection[JSONCollection]("diagram"))
  }

  override def listCanEdit(userId: UserID): Future[List[BPMNDiagram]] = {
    val query = Json.obj("canEdit" -> Json.obj("$in" -> Json.arr(BSONObjectIDFormat.writes(userId))))
    aggregateByIdAndGetNewestDiagram(query)
  }

  override def listCanView(userId: UserID): Future[List[BPMNDiagram]] = {
    val query = Json.obj("canView" -> Json.obj("$in" -> Json.arr(BSONObjectIDFormat.writes(userId))))
    aggregateByIdAndGetNewestDiagram(query)
  }

  override def listOwns(userId: UserID): Future[List[BPMNDiagram]] = {
    val query = Json.obj("owner" -> BSONObjectIDFormat.writes(userId))

    aggregateByIdAndGetNewestDiagram(query)
  }

  /**
    * Queries the collection, groups it by the diagram id and returns the latest version
    *
    * @param query selector of diagram documents
    * @return list diagrams (latest version) that match the query
    */
  private[this] def aggregateByIdAndGetNewestDiagram(query: JsObject): Future[List[BPMNDiagram]] = {
    //  QUERY
    //    db.diagram.aggregate( [{$match: query}},
    //      {$sort: {timeStamp: -1}},
    //      { $group : { _id : "$id", diagrams: { $first: "$$ROOT" } } } ] )
    for {
      collection <- collection
      dbResult <- {
        import collection.BatchCommands.AggregationFramework._
        collection.aggregate(
          Match(query),
          List(Sort(Descending("timeStamp")),
            Group(JsString("$id"))("diagram" -> First("$ROOT"))
          )
        )
      }
    } yield dbResult.firstBatch.map(json => BPMNDiagram((json \ "diagram").as[BPMNDiagram.Data]))
  }

  override def addPermissions(key: BPMNDiagramID,
                              viewers: List[UserID],
                              editors: List[UserID]): Future[Boolean] = {
    val query = Json.obj("id" -> BSONObjectIDFormat.writes(key))
    val modifier = Json.obj(
      "$addToSet" -> Json.obj(
        "canView" -> Json.obj(
          "$each" -> JsArray(viewers.map(BSONObjectIDFormat.writes))
        ),
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


  override def removePermissions(key: BPMNDiagramID,
                                 viewers: List[UserID],
                                 editors: List[UserID]): Future[Boolean] = {
    val query = Json.obj("id" -> BSONObjectIDFormat.writes(key))
    val modifier = Json.obj(
      "$pullAll" -> Json.obj(
        "canView" -> JsArray(viewers.map(BSONObjectIDFormat.writes)),
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

  override def save(value: BPMNDiagram): Future[Boolean] = {
    for {
      collection <- collection
      result <- collection.insert(
        BPMNDiagram.toData(value).copy(timeStamp = Instant.now())
      )
    } yield result.ok
  }

  override def update(value: BPMNDiagram): Future[Boolean] = {
    val query = Json.obj("id" -> BSONObjectIDFormat.writes(value.id))
    for {
      collection <- collection
      result <- collection.update(query,
        BPMNDiagram.toData(value),
        upsert = false)
    } yield result.ok
  }

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
      data <- collection
        .find(query)
        .sort(Json.obj("timeStamp" -> -1))
        .cursor[BPMNDiagram.Data]().collect[List]()
    } yield data.map(BPMNDiagram(_))
  }

  override def remove(key: BPMNDiagramID): Future[Boolean] = {
    for {
      collection <- collection
      result <- collection.remove(Json.obj("id" -> BSONObjectIDFormat.writes(key)))
    } yield result.ok
  }
}