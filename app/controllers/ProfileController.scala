package controllers

import models.User
import models.daos.UserDAO
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat
import scaldi.Injector
import util.Types.UserID

import scala.concurrent.Future

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 16/09/2016
  */
class ProfileController(implicit inj: Injector) extends ApplicationController {
  val userDAO = inject[UserDAO]

  def getProfile = silhouette.SecuredAction.async { implicit request =>
    Future.successful(
      render {
        case Accepts.Html() => Ok(views.html.profile(request.identity))
        case Accepts.Json() => Ok(Json.toJson(User.toData(request.identity)))
      }
    )
  }

  //TODO diff between public & private data
  def profile(id: UserID) = silhouette.SecuredAction.async { implicit request =>
    userDAO.findById(id).map({
      case Some(user: User) =>
        render {
          case Accepts.Html() => Ok(views.html.profile(user))
          case Accepts.Json() => Ok(Json.toJson(User.toData(user)))
        }
      case None => BadRequest("User not found!")
    })
  }

  def getProfiles = silhouette.SecuredAction.async(parse.json) { implicit request =>
    val json = request.body
    val ids = (json \ "ids").as[List[UserID]]
    userDAO.findAllById(ids).map({
      case list: List[User] => Ok(Json.toJson(list.map(User.toData)))
      case _ => BadRequest("User not found!")
    })
  }
}
