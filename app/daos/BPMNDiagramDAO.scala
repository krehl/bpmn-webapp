package daos

import models.BPMNDiagram
import org.bson.types.ObjectId
import util.Types.{BPMNDiagramID, UserID}

import scala.collection._
import scala.concurrent.Future

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 15/07/2016
  */
sealed trait BPMNDiagramDAO extends DAO[BPMNDiagramID, BPMNDiagram] {
  def list(key: UserID): Future[List[BPMNDiagram]]

//  def allKeys: Future[Set[BPMNDiagramID]]
}

class InMemoryBPMNDiagramDAO extends BPMNDiagramDAO {

  import InMemoryBPMNDiagramDAO._

  /**
    * @param value value
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
    * @return None if diagram is not present, some search result otherwise.
    */
  override def find(key: BPMNDiagramID): Future[Option[BPMNDiagram]] = {
    Future.successful(bpmnDiagrams.get(key))
  }


  override def list(key: UserID): Future[List[BPMNDiagram]] = {
    Future.successful(bpmnDiagrams.filter(_._2.owner == key).values.toList)
  }

//  override def allKeys = {Future.successful(bpmnDiagrams.keySet)}
}

object InMemoryBPMNDiagramDAO {
  val bpmnDiagrams: mutable.HashMap[BPMNDiagramID, BPMNDiagram] = mutable.HashMap()
}