package models

import java.time.Instant

import _root_.util.Types.{UserID, _}
import models.daos.{BPMNDiagramDAO, UserDAO}
import play.api.data.validation.ValidationError
import play.api.libs.json.{Json, _}
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat
import scaldi.{Injectable, Injector}

import scala.xml.{NodeSeq, XML}


/**
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/11/2016
  */

class BPMNDiagram(private val data: BPMNDiagram.Data)(implicit inj: Injector) extends Injectable {
  val bpmnDiagramDAO = inject[BPMNDiagramDAO]
  val userDAO = inject[UserDAO]

  def id = data.id

  def name = data.name

  def timeStamp = data.timeStamp

  def xmlContent = data.xmlContent

  def owner = data.owner

  def canView = data.canView

  def canEdit = data.canEdit


  //  lazy val canView = ???
  //  lazy val canEdit = ???

}

object BPMNDiagram {

  def apply(data: BPMNDiagram.Data)(implicit inj: Injector): BPMNDiagram = new BPMNDiagram(data)(inj)

  def toData(diagram: BPMNDiagram) = diagram.data

  case class Data(id: BPMNDiagramID = BSONObjectID.generate,
                  name: String,
                  timeStamp: Instant,
                  xmlContent: NodeSeq = default,
                  owner: UserID,
                  canView: Set[UserID],
                  canEdit: Set[UserID])

  private[this] val default = XML.loadString("<?xml version=\"1.0\" " +
    "encoding=\"UTF-8\"?>\n<bpmn2:definitions xmlns:xsi=\"http://www" +
    ".w3.org/2001/XMLSchema-instance\" xmlns:bpmn2=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" " +
    "xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" " +
    "xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\" xsi:schemaLocation=\"http://www.omg" +
    ".org/spec/BPMN/20100524/MODEL BPMN20.xsd\" id=\"sample-diagram\" targetNamespace=\"http://bpmn" +
    ".io/schema/bpmn\">\n    <bpmn2:process id=\"Process_1\" isExecutable=\"false\">\n        <bpmn2:startEvent " +
    "id=\"StartEvent_1\"/>\n    </bpmn2:process>\n    <bpmndi:BPMNDiagram id=\"BPMNDiagram_1\">\n        " +
    "<bpmndi:BPMNPlane id=\"BPMNPlane_1\" bpmnElement=\"Process_1\">\n            <bpmndi:BPMNShape " +
    "id=\"_BPMNShape_StartEvent_2\" bpmnElement=\"StartEvent_1\">\n                <dc:Bounds height=\"36.0\" " +
    "width=\"36.0\" x=\"412.0\" y=\"240.0\"/>\n            </bpmndi:BPMNShape>\n        </bpmndi:BPMNPlane>\n    " +
    "</bpmndi:BPMNDiagram>\n</bpmn2:definitions>")

  //------------------------------------------------------------------------------------------//
  // NodeSeq to JSON
  //------------------------------------------------------------------------------------------//
  implicit private[this] object XMLBlobWrites extends Writes[NodeSeq] {
    def writes(xml: NodeSeq) = JsString(xml.toString)
  }

  implicit private[this] object XMLBlobReads extends Reads[NodeSeq] {
    def reads(json: JsValue) = json match {
      case JsString(s) => JsSuccess(XML.loadString(s))
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jsstring"))))
    }
  }

  implicit private[this] val xmlFormat: Format[NodeSeq] = Format(XMLBlobReads, XMLBlobWrites)

  //------------------------------------------------------------------------------------------//
  // java.time.Instant to JSON
  //------------------------------------------------------------------------------------------//
  implicit private[this] object InstantWrites extends Writes[Instant] {
    def writes(stamp: Instant) = Json.obj("$date" -> stamp.toEpochMilli)
  }

  implicit private[this] object InstantReads extends Reads[Instant] {
    def reads(json: JsValue) = json match {
      case DateValue(value) => JsSuccess(Instant.ofEpochMilli(value))
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jsnumber"))))
    }

    private object DateValue {
      def unapply(obj: JsObject): Option[Long] = (obj \ "$date").asOpt[Long]
    }

  }

  implicit private[this] val instantFormat: Format[Instant] = Format(InstantReads, InstantWrites)


  //------------------------------------------------------------------------------------------//
  // BPMNDiagram to JSON
  //------------------------------------------------------------------------------------------//
  implicit val jsonFormat = Json.format[Data]

}

//
//case class BPMNDiagram(id: BPMNDiagramID = BSONObjectID.generate,
//                       name: String,
//                       timeStamp: Instant,
//                       xmlContent: NodeSeq,
//                       owner: UserID,
//                       canView: Set[UserID],
//                       canEdit: Set[UserID])
//
//object BPMNDiagram {
//  val default = XML.loadString("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<bpmn2:definitions xmlns:xsi=\"http://www" +
//    ".w3.org/2001/XMLSchema-instance\" xmlns:bpmn2=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" " +
//    "xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" " +
//    "xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\" xsi:schemaLocation=\"http://www.omg" +
//    ".org/spec/BPMN/20100524/MODEL BPMN20.xsd\" id=\"sample-diagram\" targetNamespace=\"http://bpmn" +
//    ".io/schema/bpmn\">\n    <bpmn2:process id=\"Process_1\" isExecutable=\"false\">\n        <bpmn2:startEvent " +
//    "id=\"StartEvent_1\"/>\n    </bpmn2:process>\n    <bpmndi:BPMNDiagram id=\"BPMNDiagram_1\">\n        " +
//    "<bpmndi:BPMNPlane id=\"BPMNPlane_1\" bpmnElement=\"Process_1\">\n            <bpmndi:BPMNShape " +
//    "id=\"_BPMNShape_StartEvent_2\" bpmnElement=\"StartEvent_1\">\n                <dc:Bounds height=\"36.0\" " +
//    "width=\"36.0\" x=\"412.0\" y=\"240.0\"/>\n            </bpmndi:BPMNShape>\n        </bpmndi:BPMNPlane>\n    " +
//    "</bpmndi:BPMNDiagram>\n</bpmn2:definitions>")
//
//  //------------------------------------------------------------------------------------------//
//  // NodeSeq to JSON
//  //------------------------------------------------------------------------------------------//
//  implicit private[this] object XMLBlobWrites extends Writes[NodeSeq] {
//    def writes(xml: NodeSeq) = JsString(xml.toString)
//  }
//
//  implicit private[this] object XMLBlobReads extends Reads[NodeSeq] {
//    def reads(json: JsValue) = json match {
//      case JsString(s) => JsSuccess(XML.loadString(s))
//      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jsstring"))))
//    }
//  }
//
//  implicit private[this] val xmlFormat: Format[NodeSeq] = Format(XMLBlobReads, XMLBlobWrites)
//
//  //------------------------------------------------------------------------------------------//
//  // java.time.Instant to JSON
//  //------------------------------------------------------------------------------------------//
//  implicit private[this] object InstantWrites extends Writes[Instant] {
//    def writes(stamp: Instant) = Json.obj("$date" -> stamp.toEpochMilli)
//  }
//
//  implicit private[this] object InstantReads extends Reads[Instant] {
//    def reads(json: JsValue) = json match {
//      case DateValue(value) => JsSuccess(Instant.ofEpochMilli(value))
//      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jsnumber"))))
//    }
//
//    private object DateValue {
//      def unapply(obj: JsObject): Option[Long] = (obj \ "$date").asOpt[Long]
//    }
//
//  }
//
//  implicit private[this] val instantFormat: Format[Instant] = Format(InstantReads, InstantWrites)
//
//
//  //------------------------------------------------------------------------------------------//
//  // BPMNDiagram to JSON
//  //------------------------------------------------------------------------------------------//
//  implicit val jsonFormat = Json.format[BPMNDiagram]
//}