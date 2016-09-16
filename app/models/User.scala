package models

import com.mohiva.play.silhouette.api.{Identity, LoginInfo}
import models.daos.{BPMNDiagramDAO, UserDAO}
import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat
import scaldi.{Injectable, Injector}
import util.Types._

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/4/2016
  */

class User(private val data: User.Data)(implicit inj: Injector) extends Identity with
  Injectable {
  val bpmnDiagramDAO = inject[BPMNDiagramDAO]
  val userDAO = inject[UserDAO]

  def id = data.id

  def loginInfo = data.loginInfo

  def email = data.email

  def firstName = data.firstName

  def lastName = data.lastName

  def roles = data.roles

  def ownedDiagrams = bpmnDiagramDAO.listOwns(id)

  lazy val listCanView = bpmnDiagramDAO.listCanView(id)
  lazy val listCanEdit = bpmnDiagramDAO.listCanEdit(id)
}

object User {

  def apply(data: User.Data)(implicit inj: Injector): User = new User(data)(inj)

  def toData(user: User) = user.data

  case class Data(id: UserID = BSONObjectID.generate,
                  loginInfo: LoginInfo,
                  email: String,
                  firstName: String,
                  lastName: String,
                  roles: Set[Role])

  implicit val roleFormat = Json.format[Role]
  implicit val jsonFormat = Json.format[User.Data]
}


