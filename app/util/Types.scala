package util

import reactivemongo.bson.BSONObjectID

/**
  * Useful type aliases that improve type safety
  *
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/11/2016
  */
object Types {
  type UserID = BSONObjectID
  type BPMNDiagramID = BSONObjectID
  type BPMNDiagramVersionID = BSONObjectID
  type Email = String
}
