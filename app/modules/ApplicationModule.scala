package modules

import models.daos.{BPMNDiagramDAO, InMemoryBPMNDiagramDAO}
import scaldi.Module

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 15/07/2016
  */
class ApplicationModule extends Module {
  bind[BPMNDiagramDAO] to new InMemoryBPMNDiagramDAO
}
