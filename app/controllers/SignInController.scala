package controllers

import com.mohiva.play.silhouette.api.Authenticator.Implicits._
import com.mohiva.play.silhouette.api.services.AuthenticatorResult
import com.mohiva.play.silhouette.api.util.{Clock, Credentials}
import com.mohiva.play.silhouette.api.{LoginEvent, LoginInfo}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import forms.SignInForm
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
  *         loosely based on:
  *         https://github.com/mohiva/play-silhouette-angular-seed/blob/master/app/controllers/SignInController.scala
  */
class SignInController(implicit inj: Injector) extends ApplicationController {
  val userService = inject[UserService]
  val credentialsProvider = inject[CredentialsProvider]
  val clock = inject[Clock]
  val configuration = inject[Configuration]

  /**
    * HTTP GET endpoint, requires a logged out user
    *
    * @return HTTP OK status with HTML of the sign in form page if the user is not already signed in
    */
  //TODO define fallback for unauthorized request => just do nothing
  def view = silhouette.UnsecuredAction.async {
    implicit request =>
      Future.successful(Ok(views.html.signIn(SignInForm.form, None)))
  }

  /**
    * HTTP POST endpoint, requires a logged out user
    *
    * @return Redirect or HTTP BAD_REQUEST depending on authentication success
    */
  def submit = silhouette.UnsecuredAction.async { implicit request =>
    SignInForm.form.bindFromRequest.fold(
      invalidForm => Future.successful(BadRequest(views.html.signIn(invalidForm, None))),
      validData =>
        for {
          loginInfo <- credentialsProvider.authenticate(Credentials(validData.email, validData.password))
          userOption <- userService.retrieve(loginInfo)
          futureResult <- userOption match {
            case Some(user) => grantAuthentication(validData, loginInfo, user)
            case None => Future.failed(new IdentityNotFoundException("Couldn't find user"))
            //TODO dont use excep, redirect to error page instead
          }
        } yield futureResult
    )
  }

  /**
    * Creates a new authenticator, initializes a new authentication cookie and embeds it into the
    * result
    *
    * @param data      form data, only required for remember me functionality
    * @param loginInfo user key (e-mail) & provider id
    * @param user      user that will be authenticated
    * @param request   user request
    * @return redirects to application main page, but with authentication
    */
  private[this] def grantAuthentication(data: SignInForm.Data,
                                        loginInfo: LoginInfo,
                                        user: User)
                                       (implicit request: Request[AnyContent]): Future[AuthenticatorResult] = {
    for {
      authenticator <- createAuthenticator(data, loginInfo)
      cookie <- silhouette.env.authenticatorService.init(authenticator)
      result <- silhouette.env.authenticatorService.embed(cookie, Redirect(routes.ApplicationController.index()))
    } yield {
      silhouette.env.eventBus.publish(LoginEvent(user, request))
      result
    }
  }

  /**
    * Creates a new cookie authenticator
    *
    * @param data      form data, only remember me functionality is required
    * @param loginInfo user key (e-mail) & provider id
    * @param request   user request
    * @return cookie authenticator that authenticates the user
    */
  private[this] def createAuthenticator(data: SignInForm.Data,
                                        loginInfo: LoginInfo)
                                       (implicit request: Request[AnyContent]): Future[CookieAuthenticator] = {
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
