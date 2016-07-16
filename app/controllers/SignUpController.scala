package controllers

import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.AuthenticatorResult
import com.mohiva.play.silhouette.api.util.PasswordHasher
import com.mohiva.play.silhouette.api.{LoginEvent, LoginInfo, SignUpEvent}
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import forms.SignUpForm
import models.{Customer, User}
import play.api.i18n.Messages
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, Request, Result}
import scaldi.Injector
import services.UserService

import scala.concurrent.Future

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/5/2016
  *         loosely based on
  *         https://github.com/mohiva/play-silhouette-angular-seed/blob/master/app/controllers/SignUpController.scala
  */
class SignUpController(implicit inj: Injector) extends ApplicationController {
  val userService = inject[UserService]
  val passwordHash = inject[PasswordHasher]
  val authInfoRepository = inject[AuthInfoRepository]

  /**
    * HTTP GET endpoint, requires a logged out user
    *
    * @return HTTP OK status with HTML of the sign up form if the user is not already signed in
    */
  def view = silhouette.UnsecuredAction.async { implicit request =>
    Future.successful(Ok(views.html.signUp(SignUpForm.form, None)))
  }


  /**
    * HTTP POST endpoint, requires a logged out user
    *
    * @return Redirect or HTTP BAD_REQUEST status depending on authentication success
    */
  def submit = silhouette.UnsecuredAction.async { implicit request =>
    SignUpForm.form.bindFromRequest.fold(
      invalidForm => Future.successful(BadRequest(views.html.signUp(invalidForm, None))),
      validData => createUser(validData)
    )
  }

  /**
    * Creates a user and redirects to the application main page; with the newly registered user logged
    * in. In case of failure the user stays on the sign up page.
    *
    * @param data    form data
    * @param request user request
    * @return If the e-mail is already present in the data store the user stays on the sign up page,
    *         if not he is registered, singed in and redirected to the application main page.
    */
  private[this] def createUser(data: SignUpForm.Data)
                              (implicit request: Request[AnyContent]): Future[Result] = {
    val loginInfo = LoginInfo(CredentialsProvider.ID, data.email)
    userService.retrieve(loginInfo).flatMap {
      //TODO better fallback then json string
      case Some(user) => Future.successful(BadRequest(Json.obj("message" -> Messages("user.exists"))))
      case None => createUser(loginInfo, data)
    }
  }

  /**
    * Creates and saves a new user into the underlying data store.
    *
    * @param loginInfo userService.retrieve(loginInfo).flatMap {
    * @param data      form dat, which is contains the new user information
    * @param request   user request
    * @return If the e-mail is already present in the data store the user stays on the sign up page,
    *         if not he is registered, singed in and redirected to the application main page.
    */
  private[this] def createUser(loginInfo: LoginInfo, data: SignUpForm.Data)
                              (implicit request: Request[AnyContent]): Future[AuthenticatorResult] = {
    val authInfo = passwordHash.hash(data.password)
    val user = User(
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
      cookie <- silhouette.env.authenticatorService.init(authenticator)
      result <- silhouette.env.authenticatorService.embed(cookie, Redirect(routes.ApplicationController.index()))
    } yield {
      if (saveSuccessful) {
        //TODO i do not know if its even possible that this is false?
        silhouette.env.eventBus.publish(SignUpEvent(user, request))
        silhouette.env.eventBus.publish(LoginEvent(user, request))
        result
      } else {
        //TODO better fallback then json string
        AuthenticatorResult(BadRequest(Json.obj("message" -> Messages("user.exists"))))
      }
    }
  }
}
