package util

import com.mohiva.play.silhouette.api.Env
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import models.User

/**
  * Silhouette environment that defines the user class as well as the authentication method.
  *
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/28/2016
  */
trait DefaultEnv extends Env {
  type I = User
  type A = CookieAuthenticator
}