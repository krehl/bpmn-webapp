package modules

import _root_.services.{UserIdentityService, UserService}
import _root_.util.DefaultEnv
import daos.{InMemoryPasswordDAO, InMemoryUserDAO, UserDAO}
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.actions.{SecuredAction, UnsecuredAction, UserAwareAction}
import com.mohiva.play.silhouette.api.crypto.{AuthenticatorEncoder, CookieSigner, Crypter, CrypterAuthenticatorEncoder}
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.{AuthenticatorService, AvatarService}
import com.mohiva.play.silhouette.api.util._
import com.mohiva.play.silhouette.crypto.{JcaCookieSigner, JcaCookieSignerSettings, JcaCrypter, JcaCrypterSettings}
import com.mohiva.play.silhouette.impl.authenticators._
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import com.mohiva.play.silhouette.impl.services.GravatarService
import com.mohiva.play.silhouette.impl.util.{DefaultFingerprintGenerator, PlayCacheLayer, SecureRandomIDGenerator}
import com.mohiva.play.silhouette.password.BCryptPasswordHasher
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import com.mohiva.play.silhouette.persistence.repositories.DelegableAuthInfoRepository
import daos.InMemoryUserDAO
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import net.ceedubs.ficus.readers.EnumerationReader._
import play.api.Configuration
import play.api.cache.CacheApi
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.ws.WSClient
import scaldi.Module

/**
  * Defines dependency injection wiring for the silhouette library
  *
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/4/2016
  */
class SilhouetteModule extends Module {
  // DATA STORE BINDINGS
  bind[DelegableAuthInfoDAO[PasswordInfo]] to new InMemoryPasswordDAO //TODO
  bind[UserDAO] to new InMemoryUserDAO //TODO
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
  binding to new PasswordHasherRegistry(inject[PasswordHasher])


  bind[CredentialsProvider] to new CredentialsProvider(inject[AuthInfoRepository],
    inject[PasswordHasherRegistry])

  bind[Crypter] identifiedBy 'authenticatorCrypter to new JcaCrypter(inject[JcaCrypterSettings])
  bind[JcaCrypterSettings] to inject[Configuration].underlying.as[JcaCrypterSettings]("silhouette.authenticator.crypter")
  bind[CrypterAuthenticatorEncoder] to new CrypterAuthenticatorEncoder(inject[Crypter])

  //Cookie
  bind[JcaCookieSignerSettings] to inject[Configuration].underlying.as[JcaCookieSignerSettings]("silhouette.authenticator.cookie.signer")
  bind[CookieSigner] to new JcaCookieSigner(inject[JcaCookieSignerSettings])

  bind[CookieAuthenticatorSettings] to inject[Configuration].underlying.as[CookieAuthenticatorSettings]("silhouette.authenticator")
  bind[AuthenticatorService[CookieAuthenticator]] to new CookieAuthenticatorService(
    inject[CookieAuthenticatorSettings],
    None,
    inject[CookieSigner],
    inject[AuthenticatorEncoder],
    inject[FingerprintGenerator],
    inject[IDGenerator],
    inject[Clock])

  //JWT
  //  bind[JWTAuthenticatorSettings] to inject[Configuration].underlying
  //    .as[JWTAuthenticatorSettings]("silhouette.authenticator")
  //  bind[AuthenticatorService[JWTAuthenticator]] to new JWTAuthenticatorService(
  //    inject[JWTAuthenticatorSettings],
  //    None,
  //    inject[CrypterAuthenticatorEncoder],
  //    inject[IDGenerator],
  //    inject[Clock])
}
