package controllers

import _root_.util.DefaultEnv
import forms.SignInForm
import com.mohiva.play.silhouette.api.Authenticator.Implicits._
import com.mohiva.play.silhouette.api.actions.UserAwareRequest
import com.mohiva.play.silhouette.api.util.{Clock, Credentials}
import com.mohiva.play.silhouette.api.{LoginEvent, LoginInfo}
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import models.User
import net.ceedubs.ficus.Ficus._
import play.api.Configuration
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.{AnyContent, Request}
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

  def view = silhouette.UnsecuredAction.async {
    implicit request =>
      Future.successful(Ok(views.html.signIn(SignInForm.form, None)))
  }

  def submit = silhouette.UnsecuredAction.async { implicit request =>
    SignInForm.form.bindFromRequest.fold(
      form => Future.successful(BadRequest(views.html.signIn(form, None))),
      data =>
        for {
          loginInfo <- credentialsProvider.authenticate(Credentials(data.email, data.password))
          userOption <- userService.retrieve(loginInfo)
          futureResult <- userOption match {
            case Some(user) => futureResult(data, loginInfo, user)
            case None => Future.failed(new IdentityNotFoundException("Couldn't find user")) //TODO dont use excep
          }
        } yield futureResult)
  }

  private[this] def futureResult(data: SignInForm.Data,
                                 loginInfo: LoginInfo,
                                 user: User)
                                (implicit request: Request[AnyContent]) = {
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
                                 (implicit request: Request[AnyContent]) = {
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
}
