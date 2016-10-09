package models

import java.time.Instant

import _root_.util.Types.{UserID, _}
import models.daos.{BPMNDiagramDAO, UserDAO}
import play.api.data.validation.ValidationError
import play.api.libs.json.{Json, _}
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.BSONFormats.BSONObjectIDFormat
import scaldi.{Injectable, Injector}

import scala.concurrent.Future
import scala.xml.{NodeSeq, XML}


/**
  * This object is a domain level object and represents a BPMNDiagram. It also encapsulates injected
  * database methods
  *
  * @author A. Roberto Fischer <a.robertofischer@gmail.com> on 7/11/2016
  */
class BPMNDiagram(private val data: BPMNDiagram.Data)(implicit inj: Injector) extends Injectable {
  val bpmnDiagramDAO = inject[BPMNDiagramDAO]
  val userDAO = inject[UserDAO]

  //------------------------------------------------------------------------------------------//
  // Accessors
  //------------------------------------------------------------------------------------------//
  def id = data.id

  def name = data.name

  def description = data.description

  def timeStamp = data.timeStamp

  def xmlContent = data.xmlContent

  def lastEditor = data.editor

  def owner = data.owner

  def canView = data.canView

  def canEdit = data.canEdit


  //------------------------------------------------------------------------------------------//
  // Methods with Database interaction
  //------------------------------------------------------------------------------------------//
  /**
    * Accesses the Database and returns the entire change history of this diagram
    *
    * @return Future which contains all past saves of this diagram
    */
  def history: Future[List[BPMNDiagram]] = bpmnDiagramDAO.findHistory(id)

  /**
    * Returns all User objects who can view the diagram
    *
    * @return Future which contains a list of viewers
    */
  def listUserThatCanView: Future[List[User]] = userDAO.findAllById(canView.toList)

  /**
    * Returns all User objects who can edit the diagram
    *
    * @return Future which contains a list of editors
    */
  def listUserThatCanEdit: Future[List[User]] = userDAO.findAllById(canEdit.toList)

  /**
    * Adds access permissions to this diagram and persits them
    *
    * @param viewers list of ids
    * @param editors list of ids
    * @return
    */
  def addPermissions(viewers: List[UserID], editors: List[UserID]): Future[Boolean] = {
    bpmnDiagramDAO.addPermissions(id, viewers, editors)
  }

  /**
    * Adds access permissions to this diagram and persits them
    *
    * @param viewers list of ids
    * @param editors list of ids
    * @return
    */
  def removePermissions(viewers: List[UserID], editors: List[UserID]): Future[Boolean] = {
    bpmnDiagramDAO.removePermissions(id, viewers, editors)
  }
}

/**
  * Companion object, defines static methods and vars
  */
object BPMNDiagram {

  /**
    * Constructor
    */
  def apply(data: BPMNDiagram.Data)(implicit inj: Injector): BPMNDiagram = new BPMNDiagram(data)(inj)

  /**
    * Enables ordering of diagrams by their timestamp
    *
    * @tparam A can order all subtypes of BPMNDiagram
    * @return Ordering
    */
  implicit def orderingByTimeStamp[A <: BPMNDiagram]: Ordering[A] = Ordering.by(_.timeStamp)

  def toData(diagram: BPMNDiagram) = diagram.data

  /**
    * Encapsulates the BPMNDiagram data, this separation of domain object and data may seem
    * counter intuitive, since it violates basic object oriented programming methodologies. But this
    * enables automatic transformation from and to JSON.
    */
  case class Data(id: BPMNDiagramID = BSONObjectID.generate,
                  name: String,
                  description: String,
                  timeStamp: Instant,
                  xmlContent: NodeSeq = default,
                  editor: UserID,
                  owner: UserID,
                  canView: Set[UserID],
                  canEdit: Set[UserID])

  /**
    * Empty diagram
    */
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

  /**
    * Defines how a NodeSeq (scala xml class) is transformed into JSON;
    * XML is saved as a plain JSON string
    */
  implicit private[this] object XMLBlobWrites extends Writes[NodeSeq] {
    def writes(xml: NodeSeq) = JsString(xml.toString)
  }

  /**
    * Defines how JSON is transformed into a NodeSeq
    */
  implicit private[this] object XMLBlobReads extends Reads[NodeSeq] {
    def reads(json: JsValue) = json match {
      case JsString(s) => JsSuccess(XML.loadString(s))
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jsstring"))))
    }
  }

  /**
    * Enables implicit conversion from object into JSON and vice versa
    */
  implicit private[this] val xmlFormat: Format[NodeSeq] = Format(XMLBlobReads, XMLBlobWrites)

  //------------------------------------------------------------------------------------------//
  // java.time.Instant to JSON
  //------------------------------------------------------------------------------------------//
  /**
    * Defines how a Instant (scala xml class) is transformed into JSON;
    * the time is saved as the number of milliseconds since the epoch of 1970-01-01T00:00:00Z
    */
  implicit private[this] object InstantWrites extends Writes[Instant] {
    def writes(stamp: Instant) = Json.obj("$date" -> stamp.toEpochMilli)
  }

  /**
    * Defines how a Instant (scala xml class) is transformed from JSON
    */
  implicit private[this] object InstantReads extends Reads[Instant] {
    def reads(json: JsValue) = json match {
      case DateValue(value) => JsSuccess(Instant.ofEpochMilli(value))
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jsnumber"))))
    }

    /**
      * Reads JSON with key $date as Long
      */
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