package daos

import models.BPMNDiagram
import util.Types.BPMNDiagramID

import scala.concurrent.Future
import scala.collection._

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 15/07/2016
  */
sealed trait BPMNDiagramDAO extends DAO[BPMNDiagramID, BPMNDiagram]

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
}

object InMemoryBPMNDiagramDAO {
  val bpmnDiagrams: mutable.HashMap[BPMNDiagramID, BPMNDiagram] = mutable.HashMap()
}