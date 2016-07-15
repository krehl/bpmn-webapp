package models

import org.bson.types.ObjectId
import util.Types._

import scala.xml.{NodeSeq, XML}

/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 15/07/2016
  */
case class BPMNDiagram(id: BPMNDiagramID = new ObjectId(),
                       name: String,
                       xmlContent: String,
                       owner: UserID,
                       canView: Set[UserID],
                       canEdit: Set[UserID])

object BPMNDiagram{
  val default = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<bpmn:definitions xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\" id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n  <bpmn:process id=\"Process_1\" isExecutable=\"false\">\n    <bpmn:startEvent id=\"StartEvent_1\" />\n  </bpmn:process>\n  <bpmndi:BPMNDiagram id=\"BPMNDiagram_1\">\n    <bpmndi:BPMNPlane id=\"BPMNPlane_1\" bpmnElement=\"Process_1\">\n      <bpmndi:BPMNShape id=\"_BPMNShape_StartEvent_2\" bpmnElement=\"StartEvent_1\">\n        <dc:Bounds x=\"173\" y=\"102\" width=\"36\" height=\"36\" />\n      </bpmndi:BPMNShape>\n    </bpmndi:BPMNPlane>\n  </bpmndi:BPMNDiagram>\n</bpmn:definitions>"
  val xmlContent = XML.loadString(default)
}