package controllers

import _root_.util.DefaultEnv
import com.mohiva.play.silhouette.api.Authenticator.Implicits._
import com.mohiva.play.silhouette.api.actions.UserAwareRequest
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.util.{Clock, Credentials}
import com.mohiva.play.silhouette.api.{LoginEvent, LoginInfo, Silhouette}
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import forms.SignInForm
import models.User
import net.ceedubs.ficus.Ficus._
import play.api.Configuration
import play.api.i18n.Messages
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import play.api.mvc.AnyContent
import scaldi.Injector
import services.UserService

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/5/2016
  */
class SignInController(implicit inj: Injector) extends ApplicationController {
  val userService = inject[UserService]
  val credentialsProvider = inject[CredentialsProvider]
  val clock = inject[Clock]
  val configuration = inject[Configuration]


  def submit = silhouette.UserAwareAction.async { implicit request =>
    SignInForm.form.bindFromRequest.fold(
      form => Future.successful(BadRequest(views.html.signIn(form))),
      data =>
        for {
          loginInfo <- credentialsProvider.authenticate(Credentials(data.email, data.password))
          userOption <- userService.retrieve(loginInfo)
          futureResult <- userOption match {
            case Some(user) => futureResult(data, loginInfo, user)
            case None => Future.failed(new IdentityNotFoundException("Couldn't find user"))
          }
        } yield futureResult)
  }

  private[this] def futureResult(data: SignInForm.Data,
                                 loginInfo: LoginInfo,
                                 user: User)
                                (implicit request: UserAwareRequest[DefaultEnv, AnyContent]) = {
    for {
      authenticator <- authenticator(data, loginInfo)
      token <- silhouette.env.authenticatorService.init(authenticator)
      result <- silhouette.env.authenticatorService.embed(token, Redirect(routes.ApplicationController.index()))
    } yield {
      silhouette.env.eventBus.publish(LoginEvent(user, request))
      result
    }
  }

  private[this] def authenticator(data: SignInForm.Data,
                                  loginInfo: LoginInfo)
                                 (implicit request: UserAwareRequest[DefaultEnv, AnyContent]) = {
    silhouette.env.authenticatorService.create(loginInfo).map {
      case authenticator if data.rememberMe =>
        val config = configuration.underlying
        authenticator.copy(
          expirationDateTime = clock.now + config.as[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorExpiry"),
          idleTimeout = config.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorIdleTimeout")
        )
      case authenticator => authenticator
    }
  }

  //  implicit val dataReads = (
  //    (__ \ 'email).read[String] and
  //      (__ \ 'password).read[String] and
  //      (__ \ 'rememberMe).read[Boolean]
  //    ) (SignInForm.Data.apply _)

  //  def submit = silhouette.UnsecuredAction.async(parse.json) { implicit request =>
  //    request.body.validate[SignInForm.Data].map { data =>
  //      credentialsProvider.authenticate(Credentials(data.email, data.password)).flatMap { loginInfo =>
  //        userService.retrieve(loginInfo).flatMap {
  //          case Some(user) => silhouette.env.authenticatorService.create(loginInfo).map {
  //            case authenticator if data.rememberMe =>
  //              val config = configuration.underlying
  //              authenticator.copy(
  //                expirationDateTime = clock.now + config.as[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorExpiry"),
  //                idleTimeout = config.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorIdleTimeout")
  //              )
  //            case authenticator => authenticator
  //          }.flatMap { authenticator =>
  //            silhouette.env.eventBus.publish(LoginEvent(user, request))
  //            silhouette.env.authenticatorService.init(authenticator).map { token =>
  //              Ok(Json.obj("token" -> token))
  //            }
  //          }
  //          case None => Future.failed(new IdentityNotFoundException("Couldn't find user"))
  //        }
  //      }.recover {
  //        case e: ProviderException => Unauthorized(Json.obj("message" -> Messages("invalid.credentials")))
  //      }
  //    }.recoverTotal {
  //      case error => Future.successful(Unauthorized(Json.obj("message" -> Messages("invalid.credentials"))))
  //    }
  //  }


  def view = silhouette.UnsecuredAction.async {
    implicit request =>
      Future.successful(Ok(views.html.signIn(SignInForm.form)))
  }
}
