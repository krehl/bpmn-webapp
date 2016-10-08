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
  * Controller that manages user profiles
  *
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 16/09/2016
  */
class ProfileController(implicit inj: Injector) extends ApplicationController {
  val userDAO = inject[UserDAO]

  /**
    * GET endpoint for user profile
    * @return Either JSON or HTML view of the currently logged in user profile
    */
  def getProfile = silhouette.SecuredAction.async { implicit request =>
    Future.successful(
      render {
        case Accepts.Html() => Ok(views.html.profile(request.identity))
        case Accepts.Json() => Ok(Json.toJson(User.toData(request.identity)))
      }
    )
  }

  /**
    * GET endpoint for user profile
    * @param id user id
    * @return Either JSON or HTML view of the requested user profile, BadRequest if no user was found
    */
  //TODO diff between public & private data; atm every user can view the entire profile of any other user
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

  /**
    * GET endpoint for user profiles
    * @param email user email
    * @return profile data as JSON
    */
  def profileByEmail(email: String) = silhouette.SecuredAction.async { implicit request =>
    userDAO.findByEmail(email).map({
      case Some(user: User) => Ok(Json.toJson(User.toData(user)))
      case None => BadRequest("User not found!")
    })
  }

  /**
    * GET endpoint for user profiles
    * @return multiple user profiles as a list
    */
  def getProfiles = silhouette.SecuredAction.async(parse.json) { implicit request =>
    val json = request.body
    val ids = (json \ "ids").as[List[UserID]]
    userDAO.findAllById(ids).map({
      list => Ok(Json.toJson(list.map(User.toData)))
    })
  }
}
