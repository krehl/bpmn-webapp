package models.daos

import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.Result

import scala.concurrent.Future

/**
  * Generic DAO implementation, all calls are wrapped in futures; success or failure of an operation
  * is indicated by the returned boolean value.
  *
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/7/2016
  */
trait DAO[K, V] {

  /**
    * @param value value
    * @return False if value was already present, true otherwise.
    */
  def save(value: V): Future[Boolean]

  /**
    *
    * @param value value
    * @return False if value was present, true otherwise.
    */
  def update(value: V): Future[Boolean]

  /**
    *
    * @param key key
    * @return None if value is not present, some search result otherwise.
    */
  def find(key: K): Future[Option[V]]

  /**
    *
    * @param key key
    * @return False if value was not present, true otherwise.
    */
  def remove(key: K): Future[Boolean]
}
