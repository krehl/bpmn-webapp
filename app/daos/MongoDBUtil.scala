package daos

import org.bson.types.ObjectId

import scala.util.Try

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/16/2016
  */
trait MongoDBUtil {

  def ObjectID(id: String): Try[ObjectId] = {
    Try(new ObjectId(id))
  }
}
