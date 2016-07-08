package controllers

import java.util.UUID

import com.mohiva.play.silhouette.api.actions.UserAwareRequest
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.api.{LoginEvent, LoginInfo, SignUpEvent}
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import forms.SignUpForm
import models.{Customer, User}
import play.api.i18n.Messages
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.mvc.AnyContent
import scaldi.Injector
import services.UserService
import util.DefaultEnv

import scala.concurrent.Future

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/5/2016
  */
class SignUpController(implicit inj: Injector) extends ApplicationController {
  val userService = inject[UserService]
  val passwordHash = inject[PasswordHasher]
  val authInfoRepository = inject[AuthInfoRepository]


  def submit = silhouette.UserAwareAction.async { implicit request =>
    SignUpForm.form.bindFromRequest.fold(
      form => Future.successful(BadRequest(views.html.signUp(form))),
      data => createUser(data)
    )
  }

  private[this] def createUser(data: SignUpForm.Data)(implicit request: UserAwareRequest[DefaultEnv, AnyContent]) = {
    val loginInfo = LoginInfo(CredentialsProvider.ID, data.email)
    userService.retrieve(loginInfo).flatMap {
      case Some(user) => Future.successful(BadRequest(Json.obj("message" -> Messages("user.exists"))))
      case None =>
        val authInfo = passwordHash.hash(data.password)
        val user = User(
          id = UUID.randomUUID(),
          loginInfo = loginInfo,
          firstName = data.firstName,
          lastName = data.lastName,
          email = data.email,
          roles = Set(Customer)
        )
        for {
          saveSuccessful <- userService.save(user)
          if saveSuccessful
          authInfo <- authInfoRepository.add(loginInfo, authInfo)
          authenticator <- silhouette.env.authenticatorService.create(loginInfo)
          token <- silhouette.env.authenticatorService.init(authenticator)
          result <- silhouette.env.authenticatorService.embed(token, Redirect(routes.ApplicationController.index()))
        } yield {
          silhouette.env.eventBus.publish(SignUpEvent(user, request))
          silhouette.env.eventBus.publish(LoginEvent(user, request))
          result
        }
    }
  }

  //  def submit = (silhouette.UserAwareAction andThen Action).async(parse.json) { implicit request =>
  //    request.body.validate[SignUpForm.Data].map { data =>
  //      val loginInfo = LoginInfo(CredentialsProvider.ID, data.email)
  //      userService.retrieve(loginInfo).flatMap {
  //        case Some(user) => Future.successful(BadRequest(Json.obj("message" -> Messages("user.exists"))))
  //        case None =>
  //          val authInfo = passwordHash.hash(data.password)
  //          val user = User(
  //            id = UUID.randomUUID(),
  //            loginInfo = loginInfo,
  //            firstName = data.firstName,
  //            lastName = data.lastName,
  //            email = data.email,
  //            roles = Set(Customer)
  //          )
  //          for {
  //            option <- userService.save(user)
  //            if option.isDefined
  //            authInfo <- authInfoRepository.add(loginInfo, authInfo)
  //            authenticator <- silhouette.env.authenticatorService.create(loginInfo)
  //            token <- silhouette.env.authenticatorService.init(authenticator)
  //          } yield {
  //            silhouette.env.eventBus.publish(SignUpEvent(user, request))
  //            silhouette.env.eventBus.publish(LoginEvent(user, request))
  //            Ok(Json.obj("token" -> token))
  //          }
  //      }
  //    }.recoverTotal {
  //      case error => Future.successful(Unauthorized(Json.obj("message" -> Messages("invalid.data"))))
  //    }
  //  }

  def view = {
    silhouette.UnsecuredAction.async { implicit request =>
      Future.successful(Ok(views.html.signUp(SignUpForm.form)))
    }
  }
}
