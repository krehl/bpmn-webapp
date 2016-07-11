package models

import com.mohiva.play.silhouette.api.{Identity, LoginInfo}
import org.bson.types.ObjectId
import util.Types.UserID

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/4/2016
  */
case class User(id: UserID = new ObjectId(),
                loginInfo: LoginInfo,
                email: String,
                firstName: String,
                lastName: String,
                roles: Set[Role]) extends Identity
