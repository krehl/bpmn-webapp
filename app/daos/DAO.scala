package daos

import scala.concurrent.Future

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/7/2016
  */
trait DAO[K, V] {

  /**
    * @param value
    * @return False if user was already present, true otherwise.
    */
  def save(value: V): Future[Boolean]

  /**
    *
    * @param key
    * @param value
    * @return False if user was present, true otherwise.
    */
  def update(key: K, value: V): Future[Boolean]

  /**
    *
    * @param key
    * @return None if user is not present, some search result otherwise.
    */
  def find(key: K): Future[Option[V]]

  /**
    *
    * @param key
    * @return False if user was not present, true otherwise.
    */
  def remove(key: K): Future[Boolean]
}
