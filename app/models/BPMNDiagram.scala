package models

import org.bson.types.ObjectId
import util.Types._

import scala.xml.NodeSeq

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 15/07/2016
  */
case class BPMNDiagram(id: BPMNDiagramID = new ObjectId(),
                       name: String,
                       xmlContent: NodeSeq,
                       owner: UserID,
                       canView: Set[UserID],
                       canEdit: Set[UserID])