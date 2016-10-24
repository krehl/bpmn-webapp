package models

import com.mohiva.play.silhouette.api.{Identity, LoginInfo}
import models.daos.BPMNDiagramDAO
import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat
import scaldi.{Injectable, Injector}
import util.Types._

import scala.concurrent.Future

/**
  * This object is a domain level object and represents a user. It also encapsulates injected
  * database methods
  *
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/25/2016
  */
class User(private val data: User.Data)(implicit inj: Injector) extends Identity with
  Injectable {
  val bpmnDiagramDAO = inject[BPMNDiagramDAO]

  //------------------------------------------------------------------------------------------//
  // Accessors
  //------------------------------------------------------------------------------------------//
  def id = data.id

  def loginInfo = data.loginInfo

  def email = data.email

  def firstName = data.firstName

  def lastName = data.lastName

  def ownedDiagrams = bpmnDiagramDAO.listOwns(id)

  //------------------------------------------------------------------------------------------//
  // Methods with Database interaction
  //------------------------------------------------------------------------------------------//
  /**
    * Lists all diagrams that this user can view
    * @return Future of that list
    */
  def listCanView: Future[List[BPMNDiagram]]  = bpmnDiagramDAO.listCanView(id)

  /**
    * Lists all diagrams that this user can edit
    * @return Future of that list
    */
  def listCanEdit: Future[List[BPMNDiagram]] = bpmnDiagramDAO.listCanEdit(id)
}

/**
  * Companion object, defines static methods and vars
  */
object User {

  /**
    * Constructor
    */
  def apply(data: User.Data)(implicit inj: Injector): User = new User(data)(inj)

  def toData(user: User) = user.data

  /**
    * Encapsulates the user data, this separation of domain object and data may seem
    * counter intuitive, since it violates basic object oriented programming methodologies. But this
    * enables automatic transformation from and to JSON.
    */
  case class Data(id: UserID = BSONObjectID.generate,
                  loginInfo: LoginInfo,
                  email: String,
                  firstName: String,
                  lastName: String,
                  roles: Set[Role])

  implicit val roleFormat = Json.format[Role]
  implicit val jsonFormat = Json.format[User.Data]
}


