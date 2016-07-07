package models

import java.util.UUID

import com.mohiva.play.silhouette.api.{Identity, LoginInfo}

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/4/2016
  */
case class User(id: UUID,
                loginInfo: LoginInfo,
                email: String,
                firstName: String,
                lastName: String,
                roles: Set[Role]) extends Identity
