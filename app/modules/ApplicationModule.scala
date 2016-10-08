package modules

import models.daos.{BPMNDiagramDAO, MongoBPMNDiagramDAO, MongoUserDAO, UserDAO}
import scaldi.Module

/**
  * Defines Application bindings, that are not directly related to the Silhouette library
  *
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 15/07/2016
  */
class ApplicationModule extends Module {
  bind[BPMNDiagramDAO] to new MongoBPMNDiagramDAO
  bind[UserDAO] to new MongoUserDAO
}
