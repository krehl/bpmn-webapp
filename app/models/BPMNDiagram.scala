package models

import org.bson.types.ObjectId
import util.Types.{BPMNDiagramID, UserID}

import scala.xml.XML


/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/11/2016
  */
case class BPMNDiagram(id: BPMNDiagramID = new ObjectId(),
                       name: String,
                       xmlContent: String,
                       owner: UserID,
                       canView: Set[UserID],
                       canEdit: Set[UserID])

object BPMNDiagram{
  val default = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<bpmn2:definitions xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:bpmn2=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\" xsi:schemaLocation=\"http://www.omg.org/spec/BPMN/20100524/MODEL BPMN20.xsd\" id=\"sample-diagram\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n    <bpmn2:process id=\"Process_1\" isExecutable=\"false\">\n        <bpmn2:startEvent id=\"StartEvent_1\"/>\n    </bpmn2:process>\n    <bpmndi:BPMNDiagram id=\"BPMNDiagram_1\">\n        <bpmndi:BPMNPlane id=\"BPMNPlane_1\" bpmnElement=\"Process_1\">\n            <bpmndi:BPMNShape id=\"_BPMNShape_StartEvent_2\" bpmnElement=\"StartEvent_1\">\n                <dc:Bounds height=\"36.0\" width=\"36.0\" x=\"412.0\" y=\"240.0\"/>\n            </bpmndi:BPMNShape>\n        </bpmndi:BPMNPlane>\n    </bpmndi:BPMNDiagram>\n</bpmn2:definitions>"
  val xmlContent = XML.loadString(default)
}