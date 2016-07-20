package models

import com.mohiva.play.silhouette.api.{Identity, LoginInfo}
import reactivemongo.bson.BSONObjectID
import util.Types.UserID

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/4/2016
  */
case class User(id: UserID = BSONObjectID.generate,
                loginInfo: LoginInfo,
                email: String,
                firstName: String,
                lastName: String,
                roles: Set[Role]) extends Identity





