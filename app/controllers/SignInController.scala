package controllers

import _root_.util.DefaultEnv
import com.mohiva.play.silhouette.api.Authenticator.Implicits._
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.util.{Clock, Credentials}
import com.mohiva.play.silhouette.api.{LoginEvent, Silhouette}
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import forms.SignInForm
import net.ceedubs.ficus.Ficus._
import play.api.Configuration
import play.api.i18n.Messages
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import scaldi.Injector
import services.UserService

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/5/2016
  */
class SignInController(implicit inj: Injector) extends ApplicationController {
//  val silhouette = inject[Silhouette[DefaultEnv]]
  val userService = inject[UserService]
  val credentialsProvider = inject[CredentialsProvider]
  val clock = inject[Clock]
  val configuration = inject[Configuration]


  def submit = silhouette.UserAwareAction.async { implicit request =>
    SignInForm.form.bindFromRequest.fold(
      form => Future.successful(BadRequest(views.html.signIn(form))),
      data => credentialsProvider.authenticate(Credentials(data.email, data.password)).flatMap { loginInfo =>
        userService.retrieve(loginInfo).flatMap {
          case Some(user) => silhouette.env.authenticatorService.create(loginInfo).map {
            case authenticator if data.rememberMe =>
              val config = configuration.underlying
              authenticator.copy(
                expirationDateTime = clock.now + config.as[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorExpiry"),
                idleTimeout = config.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorIdleTimeout")
              )
            case authenticator => authenticator
          }.flatMap { authenticator =>
            silhouette.env.eventBus.publish(LoginEvent(user, request))
            silhouette.env.authenticatorService.init(authenticator).map { token =>
              Ok(Json.obj("token" -> token))
            }
          }
          case None => Future.failed(new IdentityNotFoundException("Couldn't find user"))
        }
      }.recover {
        case e: ProviderException =>
          Redirect(routes.SignInController.view()).flashing("error" -> Messages("invalid.credentials"))
      })
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
