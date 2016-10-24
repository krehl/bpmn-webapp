package modules

import _root_.services.{UserIdentityService, UserService}
import _root_.util.DefaultEnv
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.actions.{SecuredAction, UnsecuredAction, UserAwareAction}
import com.mohiva.play.silhouette.api.crypto.{AuthenticatorEncoder, CookieSigner, Crypter, CrypterAuthenticatorEncoder}
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.{AuthenticatorService, AvatarService}
import com.mohiva.play.silhouette.api.util._
import com.mohiva.play.silhouette.crypto.{JcaCookieSigner, JcaCookieSignerSettings, JcaCrypter, JcaCrypterSettings}
import com.mohiva.play.silhouette.impl.authenticators.{CookieAuthenticatorSettings, _}
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import com.mohiva.play.silhouette.impl.services.GravatarService
import com.mohiva.play.silhouette.impl.util.{DefaultFingerprintGenerator, PlayCacheLayer, SecureRandomIDGenerator}
import com.mohiva.play.silhouette.password.BCryptPasswordHasher
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import com.mohiva.play.silhouette.persistence.repositories.DelegableAuthInfoRepository
import models.daos._
import play.api.cache.CacheApi
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.ws.WSClient
import scaldi.Module

import scala.concurrent.duration._
import scala.language.postfixOps


/**
  * Defines dependency injection wiring for the silhouette library
  *
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/28/2016
  */
class SilhouetteModule extends Module {
  // DATA STORE BINDINGS
  bind[DelegableAuthInfoDAO[PasswordInfo]] to new MongoPasswordDAO
  bind[CacheLayer] to new PlayCacheLayer(inject[CacheApi])

  //OTHER CLIENT SIDE BINDING
  bind[UserService] to new UserIdentityService

  //INTERNAL LIBRARY BINDINGS
  bind[Silhouette[DefaultEnv]] to new SilhouetteProvider[DefaultEnv](inject[Environment[DefaultEnv]],
    inject[SecuredAction],
    inject[UnsecuredAction],
    inject[UserAwareAction])

  bind[Environment[DefaultEnv]] to Environment[DefaultEnv](inject[UserService],
    inject[AuthenticatorService[CookieAuthenticator]],
    Seq(),
    inject[EventBus])

  bind[HTTPLayer] to new PlayHTTPLayer(inject[WSClient])
  bind[IDGenerator] to new SecureRandomIDGenerator()(defaultContext)
  bind[FingerprintGenerator] to new DefaultFingerprintGenerator(false)
  bind[EventBus] to new EventBus
  bind[Clock] to Clock()
  bind[AvatarService] to new GravatarService(inject[HTTPLayer])

  //passwords
  bind[AuthInfoRepository] to new DelegableAuthInfoRepository(inject[DelegableAuthInfoDAO[PasswordInfo]])
  bind[PasswordHasher] to new BCryptPasswordHasher
  binding to PasswordHasherRegistry(inject[PasswordHasher])


  bind[CredentialsProvider] to new CredentialsProvider(inject[AuthInfoRepository],
    inject[PasswordHasherRegistry])

  bind[Crypter] identifiedBy 'authenticatorCrypter to new JcaCrypter(inject[JcaCrypterSettings])
  //TODO for production change settings .. just a reminder!
  bind[JcaCrypterSettings] to JcaCrypterSettings("[changeme]")
  bind[CrypterAuthenticatorEncoder] to new CrypterAuthenticatorEncoder(inject[Crypter])

  //TODO for production change settings .. just a reminder!
  bind[JcaCookieSignerSettings] to JcaCookieSignerSettings("[changeme]")
  bind[CookieSigner] to new JcaCookieSigner(inject[JcaCookieSignerSettings])

  bind[CookieAuthenticatorSettings] to CookieAuthenticatorSettings(
    cookieName = "authenticator",
    cookiePath = "/",
    secureCookie = false,
    httpOnlyCookie = true,
    useFingerprinting = true,
    authenticatorIdleTimeout = Some(30 minutes),
    authenticatorExpiry = 12 hours
  )
  bind[AuthenticatorService[CookieAuthenticator]] to new CookieAuthenticatorService(
    inject[CookieAuthenticatorSettings],
    None,
    inject[CookieSigner],
    inject[AuthenticatorEncoder],
    inject[FingerprintGenerator],
    inject[IDGenerator],
    inject[Clock])
}
